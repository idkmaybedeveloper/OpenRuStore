package ru.openrustore.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import java.io.File

/** receives the outcome of a [PackageInstaller] session and brings the system dialog up. */
class InstallResultReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val packageName = intent.getStringExtra(InstallService.EXTRA_PACKAGE) ?: return
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)

    when (status) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        InstallCenter.update(packageName, InstallStatus.AwaitingConfirmation)
        val confirm =
          @Suppress("DEPRECATION") intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            ?: return
        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(confirm) }
          .onFailure { error ->
            Log.w(TAG, "cannot show the install dialog", error)
            InstallCenter.update(packageName, InstallStatus.Failed("Cannot open the install dialog"))
          }
      }

      PackageInstaller.STATUS_SUCCESS -> {
        InstallCenter.update(packageName, InstallStatus.Success)
        cleanUp(context, packageName)
      }

      else -> {
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        InstallCenter.update(packageName, InstallStatus.Failed(message ?: "Install failed (code $status)"))
        cleanUp(context, packageName)
      }
    }
  }

  private fun cleanUp(context: Context, packageName: String) {
    File(context.cacheDir, "packages/$packageName").deleteRecursively()
  }

  companion object {
    const val ACTION_RESULT = "ru.openrustore.install.RESULT"
    private const val TAG = "InstallResult"
  }
}