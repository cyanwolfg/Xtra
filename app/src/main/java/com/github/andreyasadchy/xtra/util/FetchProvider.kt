package com.github.andreyasadchy.xtra.util

import com.github.andreyasadchy.xtra.XtraApp
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import javax.inject.Inject

class FetchProvider @Inject constructor(
        private val configurationBuilder: FetchConfiguration.Builder) {

    private var instance: Fetch? = null

    fun get(videoId: Int? = null, wifiOnly: Boolean = false): Fetch {
        if (instance == null || instance!!.isClosed) {
            instance = Fetch.getInstance(
                configurationBuilder.apply {
                    XtraApp.INSTANCE.applicationContext.prefs().getInt(C.DOWNLOAD_CONCURRENT_LIMIT, 10).let { setDownloadConcurrentLimit(it) }
                    setNamespace("Fetch #$videoId")
                }.build())
        }
        return instance!!
    }
}