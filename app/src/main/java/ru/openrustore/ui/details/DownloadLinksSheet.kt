package ru.openrustore.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.openrustore.data.DownloadLinks
import ru.openrustore.ui.components.ErrorBox
import ru.openrustore.ui.components.LoadingBox
import ru.openrustore.ui.components.SectionTitle
import ru.openrustore.util.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadLinksSheet(
  links: DownloadLinks?,
  loading: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onRetry: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      SectionTitle(if (links?.isSplit == true) "Direct links (split APK)" else "Direct links")

      when {
        loading && links == null -> LoadingBox()
        error != null && links == null -> ErrorBox(error, onRetry = onRetry)
        links == null -> Unit
        else -> {
          Text(
            text = "Version ${links.versionCode}, ${links.downloadUrls.size} file(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          links.downloadUrls.forEachIndexed { index, download ->
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "${index + 1}. ${formatSize(download.size)}",
                style = MaterialTheme.typography.labelMedium,
              )
              Text(
                text = download.apkUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
              androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { copyToClipboard(context, download.apkUrl) }) { Text("Copy") }
                TextButton(onClick = { openInBrowser(context, download.apkUrl) }) { Text("Open") }
                TextButton(onClick = { shareText(context, download.apkUrl) }) { Text("Share") }
              }
            }
          }
          if (links.downloadUrls.size > 1) {
            TextButton(
              onClick = { copyToClipboard(context, links.downloadUrls.joinToString("\n") { it.apkUrl }) }
            ) {
              Text("Copy all links")
            }
          }
        }
      }
    }
  }
}