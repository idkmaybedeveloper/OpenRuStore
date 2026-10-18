package ru.openrustore

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Search : NavKey

@Serializable data class AppDetails(val packageName: String) : NavKey

@Serializable data class Reviews(val packageName: String, val appName: String) : NavKey

@Serializable data class VersionHistory(val appId: Long, val appName: String) : NavKey
