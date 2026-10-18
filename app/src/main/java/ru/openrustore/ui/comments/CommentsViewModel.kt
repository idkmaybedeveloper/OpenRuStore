package ru.openrustore.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.openrustore.data.Comment
import ru.openrustore.data.CommentSort
import ru.openrustore.data.Graph
import ru.openrustore.data.RuStoreApi
import ru.openrustore.ui.search.readable

data class CommentsUiState(
  val sort: CommentSort = CommentSort.NEW_FIRST,
  val comments: List<Comment> = emptyList(),
  val loading: Boolean = true,
  val appending: Boolean = false,
  val error: String? = null,
  val endReached: Boolean = false,
  val total: Int = 0,
)

class CommentsViewModel(
  private val packageName: String,
  private val api: RuStoreApi = Graph.api,
) : ViewModel() {

  private val _state = MutableStateFlow(CommentsUiState())
  val state: StateFlow<CommentsUiState> = _state.asStateFlow()

  private var nextPage = 0

  init {
    load(CommentSort.NEW_FIRST)
  }

  fun onSortChange(sort: CommentSort) {
    if (sort == _state.value.sort && _state.value.comments.isNotEmpty()) return
    load(sort)
  }

  fun retry() = load(_state.value.sort)

  fun loadMore() {
    val current = _state.value
    if (current.loading || current.appending || current.endReached) return

    _state.update { it.copy(appending = true, error = null) }
    viewModelScope.launch {
      runCatching { api.comments(packageName, current.sort, nextPage) }
        .onSuccess { page ->
          nextPage += 1
          _state.update {
            it.copy(
              comments = it.comments + page.content,
              appending = false,
              endReached = nextPage >= page.totalPages,
            )
          }
        }
        .onFailure { error -> _state.update { it.copy(appending = false, error = error.readable()) } }
    }
  }

  private fun load(sort: CommentSort) {
    nextPage = 0
    _state.update { it.copy(sort = sort, loading = true, error = null, comments = emptyList(), endReached = false) }
    viewModelScope.launch {
      runCatching { api.comments(packageName, sort, 0) }
        .onSuccess { page ->
          nextPage = 1
          _state.update {
            it.copy(
              comments = page.content,
              loading = false,
              endReached = page.totalPages <= 1,
              total = page.totalElements,
            )
          }
        }
        .onFailure { error ->
          _state.update { it.copy(loading = false, error = error.readable(), endReached = true) }
        }
    }
  }
}
