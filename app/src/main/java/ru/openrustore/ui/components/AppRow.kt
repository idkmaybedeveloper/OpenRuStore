package ru.openrustore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ru.openrustore.data.AppBrief
import ru.openrustore.install.InstalledApps
import ru.openrustore.util.androidVersionOf
import ru.openrustore.util.formatCount
import ru.openrustore.util.formatRating
import ru.openrustore.util.unescapeText

@Composable
fun AppRow(app: AppBrief, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val installed = remember(app.packageName) { InstalledApps.find(context, app.packageName) }

  Row(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AsyncImage(
      model = app.iconUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = app.appName,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
      )
      Text(
        text = app.packageName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RatingStars(app.averageUserRating)
        Text(
          text = "${formatRating(app.averageUserRating)} (${formatCount(app.totalRatings.toLong())})",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (app.shortDescription.isNotBlank()) {
        Text(
          text = unescapeText(app.shortDescription),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MetaChip("${androidVersionOf(app.minSdkVersion)}+")
        app.roundedDownloadsText?.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
        when {
          installed == null -> Unit
          installed.versionCode < app.versionCode -> MetaChip("Update")
          else -> MetaChip("Installed")
        }
      }
    }
  }
}