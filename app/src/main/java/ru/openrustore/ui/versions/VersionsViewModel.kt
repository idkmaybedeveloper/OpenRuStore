package ru.openrustore.ui.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.openrustore.data.AppVersion
import ru.openrustore.data.Graph
import ru.openrustore.data.RuStoreApi
import ru.openrustore.ui.search.readable

data class VersionsUiState(
  val versions: List<AppVersion> = emptyList(),
  val loading: Boolean = true,
  val appending: Boolean = false,
  val error: String? = null,
  val endReached: Boolean = false,
)

class VersionsViewModel(private val appId: Long, private val api: RuStoreApi = Graph.api) : ViewModel() {

  private val _state = MutableStateFlow(VersionsUiState())
  val state: StateFlow<VersionsUiState> = _state.asStateFlow()

  private var nextPage = 0

  init {
    retry()
  }

  fun retry() {
    nextPage = 0
    _state.update { it.copy(loading = true, error = null, versions = emptyList(), endReached = false) }
    viewModelScope.launch {
      runCatching { api.versions(appId, 0) }
        .onSuccess { page ->
          nextPage = 1
          _state.update {
            it.copy(versions = page.content, loading = false, endReached = page.totalPages <= 1)
          }
        }
        .onFailure { error ->
          _state.update { it.copy(loading = false, error = error.readable(), endReached = true) }
        }
    }
  }

  fun loadMore() {
    val current = _state.value
    if (current.loading || current.appending || current.endReached) return

    _state.update { it.copy(appending = true, error = null) }
    viewModelScope.launch {
      runCatching { api.versions(appId, nextPage) }
        .onSuccess { page ->
          nextPage += 1
          _state.update {
            it.copy(
              versions = it.versions + page.content,
              appending = false,
              endReached = nextPage >= page.totalPages,
            )
          }
        }
        .onFailure { error -> _state.update { it.copy(appending = false, error = error.readable()) } }
    }
  }
}