package ru.openrustore.install

import android.content.Context
import android.content.Intent
import android.os.Build

data class InstalledApp(val versionCode: Long, val versionName: String)

object InstalledApps {

  fun find(context: Context, packageName: String): InstalledApp? =
    runCatching {
        val info = context.packageManager.getPackageInfo(packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        InstalledApp(code, info.versionName ?: "")
      }
      .getOrNull()

  fun launchIntent(context: Context, packageName: String): Intent? =
    runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }
      .getOrNull()
      ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

  fun canInstallPackages(context: Context): Boolean =
    Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()
}