package ru.openrustore.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

object Graph {

  private lateinit var appContext: Context

  val http: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .callTimeout(0, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  val api: RuStoreApi by lazy { RuStoreApi(http) }

  val settings: Settings by lazy { Settings(appContext) }

  fun install(context: Context) {
    appContext = context.applicationContext
  }
}

class Settings(context: Context) {

  private val prefs = context.getSharedPreferences("openrustore", Context.MODE_PRIVATE)

  private val _deviceType = MutableStateFlow(readDeviceType())
  val deviceType: StateFlow<DeviceType> = _deviceType.asStateFlow()

  fun setDeviceType(value: DeviceType) {
    prefs.edit().putString(KEY_DEVICE_TYPE, value.name).apply()
    _deviceType.value = value
  }

  private fun readDeviceType(): DeviceType =
    prefs.getString(KEY_DEVICE_TYPE, null)?.let { stored -> DeviceType.entries.firstOrNull { it.name == stored } }
      ?: DeviceType.MOBILE

  private companion object {
    const val KEY_DEVICE_TYPE = "deviceType"
  }
}