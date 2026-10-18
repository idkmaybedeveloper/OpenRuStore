package ru.openrustore.ui.versions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.openrustore.ui.components.ErrorBox
import ru.openrustore.ui.components.LoadingBox
import ru.openrustore.ui.components.MessageBox
import ru.openrustore.util.formatDate
import ru.openrustore.util.unescapeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(
  appId: Long,
  appName: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: VersionsViewModel = viewModel { VersionsViewModel(appId) },
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val count = state.versions.size

  LaunchedEffect(listState, count) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
      .distinctUntilChanged()
      .collect { lastVisible -> if (count > 0 && lastVisible >= count - 3) viewModel.loadMore() }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Version history", style = MaterialTheme.typography.titleMedium)
            Text(
              text = appName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      when {
        state.loading -> LoadingBox(Modifier.align(Alignment.TopCenter))
        state.versions.isEmpty() && state.error != null ->
          ErrorBox(state.error.orEmpty(), Modifier.align(Alignment.TopCenter), onRetry = viewModel::retry)
        state.versions.isEmpty() -> MessageBox("No version history", Modifier.align(Alignment.TopCenter))
        else ->
          LazyColumn(state = listState, contentPadding = PaddingValues(16.dp)) {
            items(state.versions, key = { it.versionCode }) { version ->
              Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = version.versionName.ifBlank { version.versionCode.toString() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "(${version.versionCode})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = formatDate(version.appVerUpdatedAt),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (version.whatsNew.isNotBlank()) {
                  Text(unescapeText(version.whatsNew), style = MaterialTheme.typography.bodySmall)
                }
              }
              HorizontalDivider()
            }
            if (state.appending) item { LoadingBox() }
            state.error?.let { message -> item { ErrorBox(message, onRetry = viewModel::loadMore) } }
          }
      }
    }
  }
}