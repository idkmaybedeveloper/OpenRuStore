package ru.openrustore.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val StarColor = Color(0xFFF5A623)

@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier, starSize: Dp = 14.dp) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    repeat(5) { index ->
      val fill = (rating - index).coerceIn(0.0, 1.0).toFloat()
      Box(Modifier.size(starSize)) {
        Icon(
          imageVector = Icons.Filled.Star,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(starSize),
        )
        if (fill > 0f) {
          Box(Modifier.width(starSize * fill).height(starSize).clipToBounds()) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = null,
              tint = StarColor,
              modifier = Modifier.size(starSize),
            )
          }
        }
      }
    }
  }
}

@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier =
      modifier
        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
        .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
  }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.error,
      textAlign = TextAlign.Center,
    )
    if (onRetry != null) {
      TextButton(onClick = onRetry) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Retry", modifier = Modifier.padding(start = 6.dp))
      }
    }
  }
}

@Composable
fun MessageBox(message: String, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
  )
}

@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier, collapsedLines: Int = 5) {
  var expanded by remember(text) { mutableStateOf(false) }
  Column(modifier.animateContentSize()) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
    )
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(top = 0.dp)) {
      Text(if (expanded) "Show less" else "Read more")
    }
  }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.width(140.dp),
    )
    Text(text = value, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
fun ClickableRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
    modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 14.dp)
  ) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
  }
}
