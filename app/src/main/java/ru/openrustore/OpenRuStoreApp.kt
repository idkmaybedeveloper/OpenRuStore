package ru.openrustore

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import ru.openrustore.data.Graph

class OpenRuStoreApp : Application(), SingletonImageLoader.Factory {

  override fun onCreate() {
    super.onCreate()
    Graph.install(this)
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { Graph.http })) }
      .crossfade(true)
      .build()
}
