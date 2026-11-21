package com.gateprep.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.gateprep.app.data.Repository
import com.gateprep.app.net.ApiClient
import com.gateprep.app.net.SessionStore

/** Holds the few app-wide objects: login storage, the API client and the data repository. */
class GateApplication : Application(), ImageLoaderFactory {

    lateinit var session: SessionStore
        private set
    lateinit var api: ApiClient
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionStore(this)
        api = ApiClient(session)
        repo = Repository(this, api)
    }

    /** Images come from the backend with the user's token, and are cached on disk so they open fast and work offline. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(api.http)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(250L * 1024 * 1024).build() }
        .crossfade(true)
        .build()
}
