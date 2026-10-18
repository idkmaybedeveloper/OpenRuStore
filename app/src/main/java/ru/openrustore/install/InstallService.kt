package ru.openrustore.install

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request
import ru.openrustore.R
import ru.openrustore.data.Graph
import ru.openrustore.data.await

/**
 * Downloads the packages RuStore handed out and feeds them to [PackageInstaller]. A download is a
 * long job that must survive the user leaving a screen, so it lives in a foreground service rather
 * than in a ViewModel.
 */
class InstallService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val jobs = ConcurrentHashMap<String, Job>()

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val packageName = intent?.getStringExtra(EXTRA_PACKAGE)
    val label = intent?.getStringExtra(EXTRA_LABEL).orEmpty()
    val urls = intent?.getStringArrayExtra(EXTRA_URLS)?.toList().orEmpty()
    val totalBytes = intent?.getLongExtra(EXTRA_TOTAL_BYTES, 0L) ?: 0L

    if (packageName == null || urls.isEmpty()) {
      stopIfIdle()
      return START_NOT_STICKY
    }

    goForeground(label, null)

    if (jobs.containsKey(packageName)) return START_NOT_STICKY

    jobs[packageName] =
      scope.launch {
        try {
          val apks = download(packageName, label, urls, totalBytes)
          InstallCenter.update(packageName, InstallStatus.Installing)
          goForeground(label, null)
          commitSession(packageName, label, apks)
        } catch (error: Throwable) {
          Log.w(TAG, "install of $packageName failed", error)
          InstallCenter.update(packageName, InstallStatus.Failed(error.message ?: "Failed to download the package"))
          workDir(packageName).deleteRecursively()
        } finally {
          jobs.remove(packageName)
          stopIfIdle()
        }
      }

    return START_NOT_STICKY
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private suspend fun download(packageName: String, label: String, urls: List<String>, totalBytes: Long): List<File> {
    val dir = workDir(packageName)
    dir.deleteRecursively()
    dir.mkdirs()

    var completed = 0L
    val archives = mutableListOf<File>()

    urls.forEachIndexed { index, url ->
      val target = File(dir, "part$index.zip")
      downloadTo(url, target) { partDone, _ ->
        val progress = InstallStatus.Downloading(completed + partDone, totalBytes)
        InstallCenter.update(packageName, progress)
        goForeground(label, progress)
      }
      completed += target.length()
      archives += target
    }

    return archives.flatMap { unwrapApks(it, dir) }
  }

  private suspend fun downloadTo(url: String, target: File, onProgress: (Long, Long) -> Unit) {
    val request = Request.Builder().url(url).build()
    Graph.http.newCall(request).await().use { response ->
      if (!response.isSuccessful) error("HTTP ${response.code} while downloading")
      val total = response.body.contentLength()
      var written = 0L
      var lastReport = 0L
      response.body.byteStream().use { input ->
        target.outputStream().use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            written += read
            if (written - lastReport > REPORT_EVERY_BYTES) {
              lastReport = written
              onProgress(written, total)
            }
          }
        }
      }
      onProgress(written, total)
    }
  }

  /**
   * RuStore hands out `.zip` bundles: a single application package sits inside together with
   * baseline profiles, and app bundles arrive as one archive per split. Anything that already is
   * an APK (the CDN also serves the bare form) is passed through untouched.
   */
  private fun unwrapApks(archive: File, dir: File): List<File> {
    val extracted = mutableListOf<File>()
    ZipFile(archive).use { zip ->
      if (zip.getEntry("AndroidManifest.xml") != null) return listOf(archive)
      zip
        .entries()
        .asSequence()
        .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
        .forEach { entry ->
          val name = entry.name.substringAfterLast('/')
          val target = File(dir, "${archive.nameWithoutExtension}-$name")
          zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
          extracted += target
        }
    }
    if (extracted.isEmpty()) error("No APK inside the archive")
    archive.delete()
    return extracted
  }

  private fun commitSession(packageName: String, label: String, apks: List<File>) {
    val installer = packageManager.packageInstaller
    val params =
      PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(packageName)
      }
    val sessionId = installer.createSession(params)
    installer.openSession(sessionId).use { session ->
      apks.forEach { apk ->
        session.openWrite(apk.name, 0, apk.length()).use { output ->
          apk.inputStream().use { it.copyTo(output) }
          session.fsync(output)
        }
      }
      session.commit(resultSender(packageName, label, sessionId).intentSender)
    }
    InstallCenter.update(packageName, InstallStatus.AwaitingConfirmation)
  }

  private fun resultSender(packageName: String, label: String, sessionId: Int): PendingIntent {
    val intent =
      Intent(this, InstallResultReceiver::class.java)
        .setAction(InstallResultReceiver.ACTION_RESULT)
        .putExtra(EXTRA_PACKAGE, packageName)
        .putExtra(EXTRA_LABEL, label)
    var flags = PendingIntent.FLAG_UPDATE_CURRENT
    if (Build.VERSION.SDK_INT >= 31) flags = flags or PendingIntent.FLAG_MUTABLE
    return PendingIntent.getBroadcast(this, sessionId, intent, flags)
  }

  private fun goForeground(label: String, progress: InstallStatus.Downloading?) {
    val notification = buildNotification(label, progress)
    if (Build.VERSION.SDK_INT >= 29) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun buildNotification(label: String, progress: InstallStatus.Downloading?): Notification {
    val percent = progress?.fraction?.let { (it * 100).toInt() }
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentTitle(label.ifEmpty { getString(R.string.app_name) })
      .setContentText(if (percent != null) "Downloading: $percent%" else "Preparing to install")
      .setProgress(100, percent ?: 0, percent == null)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .build()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT < 26) return
    val channel =
      NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
        setShowBadge(false)
      }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  private fun stopIfIdle() {
    if (jobs.isEmpty()) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  private fun workDir(packageName: String) = File(cacheDir, "packages/$packageName")

  companion object {
    const val EXTRA_PACKAGE = "packageName"
    const val EXTRA_LABEL = "label"
    private const val EXTRA_URLS = "urls"
    private const val EXTRA_TOTAL_BYTES = "totalBytes"

    private const val TAG = "InstallService"
    private const val CHANNEL_ID = "downloads"
    private const val NOTIFICATION_ID = 1001
    private const val REPORT_EVERY_BYTES = 256 * 1024L

    fun start(context: Context, packageName: String, label: String, urls: List<String>, totalBytes: Long) {
      InstallCenter.update(packageName, InstallStatus.Downloading(0, totalBytes))
      val intent =
        Intent(context, InstallService::class.java)
          .putExtra(EXTRA_PACKAGE, packageName)
          .putExtra(EXTRA_LABEL, label)
          .putExtra(EXTRA_URLS, urls.toTypedArray())
          .putExtra(EXTRA_TOTAL_BYTES, totalBytes)
      ContextCompat.startForegroundService(context, intent)
    }
  }
}