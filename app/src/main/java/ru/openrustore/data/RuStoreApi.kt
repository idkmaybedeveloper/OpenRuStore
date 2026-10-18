package ru.openrustore.data

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ApiException(message: String) : IOException(message)

class RuStoreApi(private val client: OkHttpClient) {

  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
  }

  suspend fun search(query: String, page: Int, deviceType: DeviceType, pageSize: Int = 20): Page<AppBrief> =
    get(
      url =
        apiUrl("applicationData/apps")
          .addQueryParameter("pageNumber", page.toString())
          .addQueryParameter("pageSize", pageSize.toString())
          .addQueryParameter("query", query)
          .build(),
      deviceType = deviceType,
    )

  suspend fun appInfo(packageName: String, deviceType: DeviceType): AppInfo =
    get(apiUrl("applicationData/overallInfo/$packageName").build(), deviceType)

  suspend fun versions(appId: Long, page: Int, pageSize: Int = 30): Page<AppVersion> =
    get(
      url =
        apiUrl("applicationData/allAppVersionWhatsNew/$appId")
          .addQueryParameter("pageNumber", page.toString())
          .addQueryParameter("pageSize", pageSize.toString())
          .build(),
      deviceType = DeviceType.MOBILE,
    )

  suspend fun comments(packageName: String, sort: CommentSort, page: Int, pageSize: Int = 20): Page<Comment> =
    get(
      url =
        apiUrl("comment/comment")
          .addQueryParameter("packageName", packageName)
          .addQueryParameter("sortBy", sort.apiValue)
          .addQueryParameter("pageNumber", page.toString())
          .addQueryParameter("pageSize", pageSize.toString())
          .build(),
      deviceType = DeviceType.MOBILE,
    )

  /**
   * The showcase endpoint answers with a single universal package and is the one RuStore itself
   * uses first. It stays silent for apps published as an app bundle, and for those the older
   * endpoint has to be asked with the concrete device profile so that it can pick the splits.
   */
  suspend fun downloadLinks(appId: Long, deviceType: DeviceType, sdkVersion: Int): DownloadLinks {
    val showcase =
      post<DownloadLinks>(
        url = apiUrl("v3/showcase/apps/download-link").build(),
        body = """{"appId":$appId}""",
        deviceType = deviceType,
        wrapped = false,
      )
    if (showcase.downloadUrls.isNotEmpty()) return showcase

    val body =
      """
      {"appId":$appId,"screenDensity":480,"sdkVersion":$sdkVersion,"withoutSplits":false,
       "supportedAbis":["arm64-v8a","armeabi-v7a","x86_64","x86","armeabi"]}
      """
        .trimIndent()
    val legacy = post<DownloadLinks>(apiUrl("applicationData/v2/download-link").build(), body, deviceType, wrapped = true)
    if (legacy.downloadUrls.isEmpty()) throw ApiException("No download links in either response")
    return legacy
  }

  private fun apiUrl(path: String): HttpUrl.Builder = BASE_URL.toHttpUrl().newBuilder().addPathSegments(path)

  private suspend inline fun <reified T> get(url: HttpUrl, deviceType: DeviceType): T {
    val request = Request.Builder().url(url).headers(commonHeaders(deviceType)).build()
    return unwrap(client.newCall(request).await())
  }

  private suspend inline fun <reified T> post(url: HttpUrl, body: String, deviceType: DeviceType, wrapped: Boolean): T {
    val request =
      Request.Builder()
        .url(url)
        .headers(commonHeaders(deviceType))
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .build()
    val response = client.newCall(request).await()
    return if (wrapped) unwrap(response) else decode(response)
  }

  private inline fun <reified T> unwrap(response: Response): T {
    val text = response.bodyText()
    val envelope = json.decodeFromString<Envelope<T>>(text)
    if (envelope.code != "OK" || envelope.body == null) {
      throw ApiException(envelope.message ?: "RuStore answered ${envelope.code.ifEmpty { "HTTP ${response.code}" }}")
    }
    return envelope.body
  }

  private inline fun <reified T> decode(response: Response): T = json.decodeFromString(response.bodyText())

  private fun Response.bodyText(): String =
    use {
      val text = body.string()
      if (!isSuccessful && text.isBlank()) throw ApiException("HTTP $code")
      text
    }

  private fun commonHeaders(deviceType: DeviceType) =
    okhttp3.Headers.Builder()
      .add("deviceType", deviceType.apiValue)
      .add("ruStoreVerCode", RUSTORE_VER_CODE)
      .add("Accept", "application/json")
      .build()

  companion object {
    const val BASE_URL = "https://backapi.rustore.ru/"

    // NOTE: the backend hides parts of its answers (download links above all) from clients that do
    // not claim to be a recent RuStore build, so every request carries the version of one
    private const val RUSTORE_VER_CODE = "1105001"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
  }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
  enqueue(
    object : Callback {
      override fun onResponse(call: Call, response: Response) = continuation.resume(response)

      override fun onFailure(call: Call, e: IOException) {
        if (!continuation.isCancelled) continuation.resumeWithException(e)
      }
    }
  )
  continuation.invokeOnCancellation { runCatching { cancel() } }
}