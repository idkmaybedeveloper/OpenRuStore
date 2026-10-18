package ru.openrustore.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.openrustore.data.AppBrief
import ru.openrustore.data.DeviceType
import ru.openrustore.data.Graph
import ru.openrustore.data.RuStoreApi
import ru.openrustore.data.Settings

data class SearchUiState(
  val query: String = "",
  val deviceType: DeviceType = DeviceType.MOBILE,
  val apps: List<AppBrief> = emptyList(),
  val loading: Boolean = false,
  val appending: Boolean = false,
  val error: String? = null,
  val endReached: Boolean = true,
)

@OptIn(FlowPreview::class)
class SearchViewModel(
  private val api: RuStoreApi = Graph.api,
  private val settings: Settings = Graph.settings,
) : ViewModel() {

  private val queryInput = MutableStateFlow("")
  private val _state = MutableStateFlow(SearchUiState(deviceType = settings.deviceType.value))
  val state: StateFlow<SearchUiState> = _state.asStateFlow()

  private var nextPage = 0

  init {
    viewModelScope.launch {
      combine(queryInput.debounce(400), settings.deviceType) { query, device -> query.trim() to device }
        .distinctUntilChanged()
        .collect { (query, device) -> loadFirstPage(query, device) }
    }
  }

  fun onQueryChange(value: String) {
    queryInput.value = value
    _state.update { it.copy(query = value) }
  }

  fun onDeviceTypeChange(deviceType: DeviceType) {
    settings.setDeviceType(deviceType)
    _state.update { it.copy(deviceType = deviceType) }
  }

  fun retry() {
    viewModelScope.launch { loadFirstPage(_state.value.query.trim(), _state.value.deviceType) }
  }

  fun loadMore() {
    val current = _state.value
    if (current.loading || current.appending || current.endReached || current.query.isBlank()) return

    _state.update { it.copy(appending = true, error = null) }
    viewModelScope.launch {
      runCatching { api.search(current.query.trim(), nextPage, current.deviceType) }
        .onSuccess { page ->
          nextPage += 1
          _state.update {
            it.copy(
              apps = it.apps + page.content,
              appending = false,
              endReached = nextPage >= page.totalPages,
            )
          }
        }
        .onFailure { error -> _state.update { it.copy(appending = false, error = error.readable()) } }
    }
  }

  private suspend fun loadFirstPage(query: String, deviceType: DeviceType) {
    nextPage = 0
    if (query.isBlank()) {
      _state.update {
        it.copy(deviceType = deviceType, apps = emptyList(), loading = false, error = null, endReached = true)
      }
      return
    }

    _state.update { it.copy(deviceType = deviceType, loading = true, error = null, apps = emptyList()) }
    runCatching { api.search(query, 0, deviceType) }
      .onSuccess { page ->
        nextPage = 1
        _state.update {
          it.copy(apps = page.content, loading = false, endReached = page.totalPages <= 1)
        }
      }
      .onFailure { error -> _state.update { it.copy(loading = false, error = error.readable(), endReached = true) } }
  }
}

internal fun Throwable.readable(): String = message?.takeIf { it.isNotBlank() } ?: "Cannot reach RuStore"