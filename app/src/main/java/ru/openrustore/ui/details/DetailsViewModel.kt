package ru.openrustore.ui.details

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.openrustore.data.AppInfo
import ru.openrustore.data.DownloadLinks
import ru.openrustore.data.Graph
import ru.openrustore.data.RuStoreApi
import ru.openrustore.data.Settings
import ru.openrustore.install.InstallService
import ru.openrustore.ui.search.readable

data class DetailsUiState(
  val loading: Boolean = true,
  val info: AppInfo? = null,
  val error: String? = null,
  val links: DownloadLinks? = null,
  val linksLoading: Boolean = false,
  val linksError: String? = null,
)

class DetailsViewModel(
  private val packageName: String,
  private val api: RuStoreApi = Graph.api,
  settings: Settings = Graph.settings,
) : ViewModel() {

  private val deviceType = settings.deviceType.value

  private val _state = MutableStateFlow(DetailsUiState())
  val state: StateFlow<DetailsUiState> = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
      runCatching { api.appInfo(packageName, deviceType) }
        .onSuccess { info -> _state.update { it.copy(loading = false, info = info) } }
        .onFailure { error -> _state.update { it.copy(loading = false, error = error.readable()) } }
    }
  }

  fun loadLinks() {
    val info = _state.value.info ?: return
    if (_state.value.linksLoading) return

    _state.update { it.copy(linksLoading = true, linksError = null) }
    viewModelScope.launch {
      resolveLinks(info)
        .onSuccess { links -> _state.update { it.copy(linksLoading = false, links = links) } }
        .onFailure { error -> _state.update { it.copy(linksLoading = false, linksError = error.readable()) } }
    }
  }

  fun install(context: Context) {
    val info = _state.value.info ?: return

    _state.update { it.copy(linksLoading = true, linksError = null) }
    viewModelScope.launch {
      resolveLinks(info)
        .onSuccess { links ->
          _state.update { it.copy(linksLoading = false, links = links) }
          InstallService.start(
            context = context,
            packageName = info.packageName,
            label = info.appName,
            urls = links.downloadUrls.map { it.url },
            totalBytes = links.downloadUrls.sumOf { it.size },
          )
        }
        .onFailure { error -> _state.update { it.copy(linksLoading = false, linksError = error.readable()) } }
    }
  }

  private suspend fun resolveLinks(info: AppInfo): Result<DownloadLinks> =
    _state.value.links?.let { Result.success(it) }
      ?: runCatching { api.downloadLinks(info.appId, deviceType, Build.VERSION.SDK_INT) }
}