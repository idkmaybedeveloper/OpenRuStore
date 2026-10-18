package ru.openrustore.ui.comments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import ru.openrustore.data.Comment
import ru.openrustore.data.CommentSort
import ru.openrustore.ui.components.ErrorBox
import ru.openrustore.ui.components.LoadingBox
import ru.openrustore.ui.components.MessageBox
import ru.openrustore.ui.components.RatingStars
import ru.openrustore.util.formatCount
import ru.openrustore.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
  packageName: String,
  appName: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CommentsViewModel = viewModel { CommentsViewModel(packageName) },
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val count = state.comments.size

  LaunchedEffect(listState, count) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
      .distinctUntilChanged()
      .collect { lastVisible -> if (count > 0 && lastVisible >= count - 3) viewModel.loadMore() }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      Column {
        TopAppBar(
          title = {
            Column {
              Text(appName, maxLines = 1, style = MaterialTheme.typography.titleMedium)
              if (state.total > 0) {
                Text(
                  text = "${formatCount(state.total.toLong())} reviews",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(CommentSort.entries) { sort ->
            FilterChip(
              selected = sort == state.sort,
              onClick = { viewModel.onSortChange(sort) },
              label = { Text(sort.label) },
            )
          }
        }
      }
    },
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      when {
        state.loading -> LoadingBox(Modifier.align(Alignment.TopCenter))
        state.comments.isEmpty() && state.error != null ->
          ErrorBox(state.error.orEmpty(), Modifier.align(Alignment.TopCenter), onRetry = viewModel::retry)
        state.comments.isEmpty() -> MessageBox("No reviews yet", Modifier.align(Alignment.TopCenter))
        else ->
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.comments, key = { it.commentId }) { comment -> CommentCard(comment) }
            if (state.appending) item { LoadingBox() }
            state.error?.let { message -> item { ErrorBox(message, onRetry = viewModel::loadMore) } }
          }
      }
    }
  }
}

@Composable
private fun CommentCard(comment: Comment) {
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = comment.firstName.ifBlank { "Anonymous" },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        RatingStars(comment.appRating.toDouble())
        Text(
          text = formatDate(comment.commentDate),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (comment.commentText.isNotBlank()) {
        Text(comment.commentText, style = MaterialTheme.typography.bodyMedium)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("+${comment.likeCounter}", style = MaterialTheme.typography.labelSmall)
        Text("-${comment.dislikeCounter}", style = MaterialTheme.typography.labelSmall)
      }
      comment.devResponse?.takeIf { it.isNotBlank() }?.let { response ->
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "Developer reply, ${formatDate(comment.devResponseDate)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(response, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}