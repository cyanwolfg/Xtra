package com.github.exact7.xtra.repository.datasource

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import com.github.exact7.xtra.api.KrakenApi
import com.github.exact7.xtra.model.kraken.video.BroadcastType
import com.github.exact7.xtra.model.kraken.video.Sort
import com.github.exact7.xtra.model.kraken.video.Video

class ChannelVideosDataSource (
        private val channelId: String,
        private val broadcastTypes: BroadcastType,
        private val sort: Sort,
        private val api: KrakenApi) : BasePositionalDataSource<Video>() {

    override fun loadInitial(params: PositionalDataSource.LoadInitialParams, callback: PositionalDataSource.LoadInitialCallback<Video>) {
        super.loadInitial(params, callback)
        api.getChannelVideos(channelId, broadcastTypes, sort, params.requestedLoadSize, 0)
                .subscribe({ callback.onSuccess(it.videos) }, { callback.onFailure(it, params) })
                .addTo(compositeDisposable)
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams, callback: PositionalDataSource.LoadRangeCallback<Video>) {
        super.loadRange(params, callback)
        api.getChannelVideos(channelId, broadcastTypes, sort, params.loadSize, params.startPosition)
                .subscribe({ callback.onSuccess(it.videos) }, { callback.onFailure(it, params) })
                .addTo(compositeDisposable)
    }

    class Factory(
            private val channelId: String,
            private val broadcastTypes: BroadcastType,
            private val sort: Sort,
            private val api: KrakenApi) : BaseDataSourceFactory<Int, Video, ChannelVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                ChannelVideosDataSource(channelId, broadcastTypes, sort, api).also(sourceLiveData::postValue)
    }
}
