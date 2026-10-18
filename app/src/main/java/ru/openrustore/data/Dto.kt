package ru.openrustore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(val code: String = "", val message: String? = null, val body: T? = null)

@Serializable
data class Page<T>(
  val content: List<T> = emptyList(),
  val pageNumber: Int = 0,
  val pageSize: Int = 0,
  val totalElements: Int = 0,
  val totalPages: Int = 0,
)

@Serializable
data class AppBrief(
  val appId: Long = 0,
  val packageName: String = "",
  val appName: String = "",
  val shortDescription: String = "",
  val iconUrl: String = "",
  val appType: String = "",
  val minSdkVersion: Int = 0,
  val versionCode: Long = 0,
  val price: Long = 0,
  val averageUserRating: Double = 0.0,
  val totalRatings: Int = 0,
  val roundedDownloadsText: String? = null,
)

@Serializable
data class AppInfo(
  val appId: Long = 0,
  val packageName: String = "",
  val appName: String = "",
  val companyName: String = "",
  val categories: List<String> = emptyList(),
  val shortDescription: String = "",
  val fullDescription: String = "",
  val whatsNew: String = "",
  val iconUrl: String = "",
  val fileUrls: List<FileUrl> = emptyList(),
  val fileSize: Long = 0,
  val versionName: String = "",
  val versionCode: Long = 0,
  val versionId: Long = 0,
  val minSdkVersion: Int = 0,
  val targetSdkVersion: Int = 0,
  val minAndroidVersion: String? = null,
  val downloads: Long = 0,
  val roundedDownloadsText: String? = null,
  val price: Long = 0,
  val appVerUpdatedAt: String? = null,
  val firstPublishedAt: String? = null,
  val ageLegal: String? = null,
  val signature: String? = null,
  val rating: Rating = Rating(),
  val aggregatorInfo: AggregatorInfo? = null,
  val deviceTypes: List<String> = emptyList(),
) {
  val screenshots: List<String>
    get() = fileUrls.filter { it.type == "SCREENSHOT" }.sortedBy { it.ordinal }.map { it.fileUrl }
}

@Serializable
data class FileUrl(val fileUrl: String = "", val ordinal: Int = 0, val type: String = "")

@Serializable
data class Rating(val average: Double = 0.0, val votes: Int = 0)

@Serializable
data class AggregatorInfo(val companyName: String? = null, val displayedSource: String? = null)

@Serializable
data class AppVersion(
  val versionName: String = "",
  val versionCode: Long = 0,
  val whatsNew: String = "",
  val appVerUpdatedAt: String? = null,
)

@Serializable
data class Comment(
  val commentId: Long = 0,
  val appRating: Int = 0,
  val firstName: String = "",
  val commentDate: String? = null,
  val commentText: String = "",
  val likeCounter: Int = 0,
  val dislikeCounter: Int = 0,
  val userVerCode: Long = 0,
  val devResponse: String? = null,
  val devResponseDate: String? = null,
)

@Serializable
data class DownloadLinks(
  val appId: Long = 0,
  val versionCode: Long = 0,
  val versionId: Long = 0,
  val downloadUrls: List<DownloadUrl> = emptyList(),
  @SerialName("preApproveInfo") val preApprove: PreApproveInfo? = null,
) {
  val isSplit: Boolean
    get() = downloadUrls.size > 1
}

@Serializable
data class DownloadUrl(val url: String = "", val size: Long = 0, val hash: String? = null) {
  /**
   * The cdn serves the very same object under both extensions: `.zip` wraps the package into an
   * archive, while `.apk` returns the bare APK. The archive form is what the installer consumes,
   * the bare form is what a browser or a file manager can open, so both are kept around.
   */
  val apkUrl: String
    get() = if (url.endsWith(".zip")) url.removeSuffix(".zip") + ".apk" else url
}

@Serializable
data class PreApproveInfo(val label: String? = null, val locale: String? = null)

enum class CommentSort(val apiValue: String, val label: String) {
  NEW_FIRST("NEW_FIRST", "Newest"),
  USEFUL_FIRST("USEFUL_FIRST", "Most useful"),
  POSITIVE_FIRST("POSITIVE_FIRST", "Positive first"),
  NEGATIVE_FIRST("NEGATIVE_FIRST", "Negative first"),
}

enum class DeviceType(val apiValue: String, val label: String) {
  MOBILE("mobile", "Mobile"),
  TV("tv", "TV"),
}