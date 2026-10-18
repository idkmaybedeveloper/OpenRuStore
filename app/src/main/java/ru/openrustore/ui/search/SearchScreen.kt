package ru.openrustore.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.openrustore.data.AppBrief
import ru.openrustore.data.DeviceType
import ru.openrustore.ui.components.AppRow
import ru.openrustore.ui.components.ErrorBox
import ru.openrustore.ui.components.LoadingBox
import ru.openrustore.ui.components.MessageBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  onAppClick: (AppBrief) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SearchViewModel = viewModel { SearchViewModel() },
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val appCount = state.apps.size

  LaunchedEffect(listState, appCount) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
      .distinctUntilChanged()
      .collect { lastVisible -> if (appCount > 0 && lastVisible >= appCount - 4) viewModel.loadMore() }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      Column {
        TopAppBar(title = { Text("OpenRuStore") })
        OutlinedTextField(
          value = state.query,
          onValueChange = viewModel::onQueryChange,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          placeholder = { Text("Search apps") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (state.query.isNotEmpty()) {
              IconButton(onClick = { viewModel.onQueryChange("") }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        DeviceTypeSelector(
          selected = state.deviceType,
          onSelect = viewModel::onDeviceTypeChange,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    },
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      when {
        state.loading -> LoadingBox(Modifier.align(Alignment.TopCenter))
        state.apps.isEmpty() && state.error != null ->
          ErrorBox(state.error.orEmpty(), Modifier.align(Alignment.TopCenter), onRetry = viewModel::retry)
        state.apps.isEmpty() && state.query.isBlank() ->
          MessageBox("Type an app name to start", Modifier.align(Alignment.TopCenter))
        state.apps.isEmpty() -> MessageBox("Nothing found", Modifier.align(Alignment.TopCenter))
        else ->
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            items(state.apps, key = { it.appId }) { app -> AppRow(app = app, onClick = { onAppClick(app) }) }
            if (state.appending) item { LoadingBox() }
            state.error?.let { message -> item { ErrorBox(message, onRetry = viewModel::loadMore) } }
          }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceTypeSelector(
  selected: DeviceType,
  onSelect: (DeviceType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val options = remember { DeviceType.entries }
  SingleChoiceSegmentedButtonRow(modifier) {
    options.forEachIndexed { index, option ->
      SegmentedButton(
        selected = option == selected,
        onClick = { onSelect(option) },
        shape = SegmentedButtonDefaults.itemShape(index, options.size),
      ) {
        Text(option.label, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}