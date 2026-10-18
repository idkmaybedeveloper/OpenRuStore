package ru.openrustore.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

@Composable
fun ScreenshotGallery(screenshots: List<String>, startIndex: Int, onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { screenshots.size })

    Box(Modifier.fillMaxSize().background(Color.Black)) {
      HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        ZoomableImage(url = screenshots[page])
      }
      IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
        Icon(Icons.Filled.Clear, contentDescription = "Close", tint = Color.White)
      }
      Text(
        text = "${pagerState.currentPage + 1} / ${screenshots.size}",
        color = Color.White,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      )
    }
  }
}

@Composable
private fun ZoomableImage(url: String) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  val transformState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 5f)
    offset = if (scale > 1f) offset + panChange else Offset.Zero
  }

  AsyncImage(
    model = url,
    contentDescription = null,
    contentScale = ContentScale.Fit,
    modifier =
      Modifier.fillMaxSize()
        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        .transformable(transformState)
        .pointerInput(url) {
          detectTapGestures(
            onDoubleTap = {
              scale = if (scale > 1f) 1f else 2.5f
              offset = Offset.Zero
            }
          )
        },
  )
}