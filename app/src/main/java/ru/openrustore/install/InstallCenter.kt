package ru.openrustore.install

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface InstallStatus {

  data class Downloading(val downloaded: Long, val total: Long) : InstallStatus {
    val fraction: Float?
      get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
  }

  data object Installing : InstallStatus

  data object AwaitingConfirmation : InstallStatus

  data object Success : InstallStatus

  data class Failed(val message: String) : InstallStatus

  val isBusy: Boolean
    get() = this is Downloading || this is Installing || this is AwaitingConfirmation
}

/** single place the install service publishes progress to and every screen observes. */
object InstallCenter {

  private val _statuses = MutableStateFlow<Map<String, InstallStatus>>(emptyMap())
  val statuses: StateFlow<Map<String, InstallStatus>> = _statuses.asStateFlow()

  fun update(packageName: String, status: InstallStatus) {
    _statuses.value = _statuses.value + (packageName to status)
  }

  fun forget(packageName: String) {
    _statuses.value = _statuses.value - packageName
  }

  fun statusOf(packageName: String): InstallStatus? = _statuses.value[packageName]
}