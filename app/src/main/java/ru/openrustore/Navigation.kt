package ru.openrustore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ru.openrustore.ui.comments.CommentsScreen
import ru.openrustore.ui.details.DetailsScreen
import ru.openrustore.ui.search.SearchScreen
import ru.openrustore.ui.versions.VersionsScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(Search)

  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = { backStack.removeLastOrNull() },
    // NOTE: the view model decorator scopes a ViewModel to its own entry, so every app opened from
    // search gets a fresh one instead of reusing the previous screen's state.
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<Search> { SearchScreen(onAppClick = { app -> backStack.add(AppDetails(app.packageName)) }) }
        entry<AppDetails> { key ->
          DetailsScreen(
            packageName = key.packageName,
            onBack = { backStack.removeLastOrNull() },
            onOpenComments = { info -> backStack.add(Reviews(info.packageName, info.appName)) },
            onOpenVersions = { info -> backStack.add(VersionHistory(info.appId, info.appName)) },
          )
        }
        entry<Reviews> { key ->
          CommentsScreen(
            packageName = key.packageName,
            appName = key.appName,
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<VersionHistory> { key ->
          VersionsScreen(appId = key.appId, appName = key.appName, onBack = { backStack.removeLastOrNull() })
        }
      },
  )
}
