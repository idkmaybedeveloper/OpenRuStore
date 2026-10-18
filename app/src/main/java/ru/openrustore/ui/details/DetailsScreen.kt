package ru.openrustore.ui.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import ru.openrustore.data.AppInfo
import ru.openrustore.install.InstallCenter
import ru.openrustore.install.InstallStatus
import ru.openrustore.install.InstalledApps
import ru.openrustore.ui.components.ClickableRow
import ru.openrustore.ui.components.ErrorBox
import ru.openrustore.ui.components.ExpandableText
import ru.openrustore.ui.components.KeyValueRow
import ru.openrustore.ui.components.LoadingBox
import ru.openrustore.ui.components.MetaChip
import ru.openrustore.ui.components.RatingStars
import ru.openrustore.ui.components.SectionTitle
import ru.openrustore.util.androidVersionOf
import ru.openrustore.util.formatCount
import ru.openrustore.util.formatDate
import ru.openrustore.util.formatRating
import ru.openrustore.util.formatSize
import ru.openrustore.util.unescapeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
  packageName: String,
  onBack: () -> Unit,
  onOpenComments: (AppInfo) -> Unit,
  onOpenVersions: (AppInfo) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DetailsViewModel = viewModel { DetailsViewModel(packageName) },
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val statuses by InstallCenter.statuses.collectAsStateWithLifecycle()
  var galleryIndex by remember { mutableStateOf<Int?>(null) }
  var linksVisible by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(state.info?.appName ?: "App", maxLines = 1) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { padding ->
    val info = state.info
    Box(Modifier.padding(padding).fillMaxSize()) {
      when {
        state.loading -> LoadingBox(Modifier.align(Alignment.TopCenter))
        info == null -> ErrorBox(state.error ?: "App not found", onRetry = viewModel::load)
        else ->
          AppDetails(
            info = info,
            status = statuses[info.packageName],
            linksLoading = state.linksLoading,
            linksError = state.linksError,
            onInstall = viewModel::install,
            onShowLinks = {
              linksVisible = true
              viewModel.loadLinks()
            },
            onScreenshotClick = { index -> galleryIndex = index },
            onOpenComments = { onOpenComments(info) },
            onOpenVersions = { onOpenVersions(info) },
          )
      }
    }
  }

  val screenshots = state.info?.screenshots.orEmpty()
  galleryIndex?.let { index ->
    if (screenshots.isNotEmpty()) {
      ScreenshotGallery(screenshots = screenshots, startIndex = index, onDismiss = { galleryIndex = null })
    }
  }

  if (linksVisible) {
    DownloadLinksSheet(
      links = state.links,
      loading = state.linksLoading,
      error = state.linksError,
      onDismiss = { linksVisible = false },
      onRetry = viewModel::loadLinks,
    )
  }
}

@Composable
private fun AppDetails(
  info: AppInfo,
  status: InstallStatus?,
  linksLoading: Boolean,
  linksError: String?,
  onInstall: (Context) -> Unit,
  onShowLinks: () -> Unit,
  onScreenshotClick: (Int) -> Unit,
  onOpenComments: () -> Unit,
  onOpenVersions: () -> Unit,
) {
  val context = LocalContext.current
  val installed = remember(info.packageName, status) { InstalledApps.find(context, info.packageName) }
  var sourcesWarning by remember { mutableStateOf(false) }

  LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AsyncImage(
          model = info.iconUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(80.dp).clip(RoundedCornerShape(18.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(info.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
          Text(
            text = info.aggregatorInfo?.companyName?.takeIf { it.isNotBlank() } ?: info.companyName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RatingStars(info.rating.average)
            Text(
              text = "${formatRating(info.rating.average)} (${formatCount(info.rating.votes.toLong())})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = info.packageName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MetaChip("${androidVersionOf(info.minSdkVersion)}+")
        MetaChip(formatSize(info.fileSize))
        MetaChip(info.versionName)
        info.ageLegal?.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
      }
    }

    item {
      InstallActions(
        info = info,
        status = status,
        installedVersionCode = installed?.versionCode,
        busy = linksLoading,
        onInstall = {
          if (InstalledApps.canInstallPackages(context)) onInstall(context) else sourcesWarning = true
        },
        onOpen = { InstalledApps.launchIntent(context, info.packageName)?.let(context::startActivity) },
        onShowLinks = onShowLinks,
      )
    }

    if (linksError != null) {
      item { Text(linksError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
    }

    if (info.screenshots.isNotEmpty()) {
      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          itemsIndexed(info.screenshots) { index, url ->
            AsyncImage(
              model = url,
              contentDescription = null,
              contentScale = ContentScale.Fit,
              modifier =
                Modifier.height(220.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onScreenshotClick(index) },
            )
          }
        }
      }
    }

    if (info.whatsNew.isNotBlank()) {
      item {
        Column(Modifier.padding(horizontal = 16.dp)) {
          SectionTitle("What's new")
          ExpandableText(unescapeText(info.whatsNew), collapsedLines = 4)
        }
      }
    }

    if (info.fullDescription.isNotBlank()) {
      item {
        Column(Modifier.padding(horizontal = 16.dp)) {
          SectionTitle("Description")
          ExpandableText(unescapeText(info.fullDescription))
        }
      }
    }

    item {
      Column(Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Details")
        KeyValueRow("App ID", info.appId.toString())
        KeyValueRow("Version", "${info.versionName} (${info.versionCode})")
        KeyValueRow("Size", formatSize(info.fileSize))
        KeyValueRow("Requires", androidVersionOf(info.minSdkVersion))
        if (info.targetSdkVersion > 0) KeyValueRow("Targets", androidVersionOf(info.targetSdkVersion))
        KeyValueRow("Downloads", info.roundedDownloadsText ?: formatCount(info.downloads))
        KeyValueRow("Updated", formatDate(info.appVerUpdatedAt))
        KeyValueRow("Published", formatDate(info.firstPublishedAt))
        if (info.categories.isNotEmpty()) KeyValueRow("Categories", info.categories.joinToString())
        installed?.let { KeyValueRow("Installed", "${it.versionName} (${it.versionCode})") }
        info.aggregatorInfo?.displayedSource?.let { KeyValueRow("Source", it) }
      }
    }

    item {
      Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        HorizontalDivider()
        ClickableRow("Ratings and reviews", onOpenComments)
        HorizontalDivider()
        ClickableRow("Version history", onOpenVersions)
        HorizontalDivider()
      }
    }
  }

  if (sourcesWarning) {
    UnknownSourcesDialog(
      onDismiss = { sourcesWarning = false },
      onOpenSettings = {
        sourcesWarning = false
        val intent =
          Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
      },
    )
  }
}

@Composable
private fun InstallActions(
  info: AppInfo,
  status: InstallStatus?,
  installedVersionCode: Long?,
  busy: Boolean,
  onInstall: () -> Unit,
  onOpen: () -> Unit,
  onShowLinks: () -> Unit,
) {
  val downloading = status as? InstallStatus.Downloading
  val label =
    when {
      busy && status == null -> "Resolving links"
      downloading != null -> {
        val percent = downloading.fraction?.let { " ${(it * 100).toInt()}%" }.orEmpty()
        "Downloading$percent"
      }
      status is InstallStatus.Installing -> "Installing"
      status is InstallStatus.AwaitingConfirmation -> "Confirm the install"
      installedVersionCode == null -> "Install"
      installedVersionCode < info.versionCode -> "Update"
      else -> "Reinstall"
    }

  Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = onInstall, enabled = status?.isBusy != true && !busy, modifier = Modifier.weight(1f)) {
        Text(label)
      }
      if (installedVersionCode != null) {
        OutlinedButton(onClick = onOpen) { Text("Open") }
      }
    }
    if (downloading != null) {
      val fraction = downloading.fraction
      if (fraction != null) {
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
      } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
      }
    }
    (status as? InstallStatus.Failed)?.let { failure ->
      Text(failure.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (status is InstallStatus.Success) {
      Text("Installed", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = onShowLinks) { Text("APK links") }
  }
}

@Composable
private fun UnknownSourcesDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Permission needed") },
    text = { Text("Allow OpenRuStore to install apps, otherwise the system refuses every APK it hands over.") },
    confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open settings") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

internal fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText("OpenRuStore", text))
}

internal fun shareText(context: Context, text: String) {
  val intent =
    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
  context.startActivity(Intent.createChooser(intent, "Share link"))
}

internal fun openInBrowser(context: Context, url: String) {
  runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
}