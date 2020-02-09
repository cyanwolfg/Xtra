package com.github.exact7.xtra.repository.datasource

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import com.github.exact7.xtra.api.KrakenApi
import com.github.exact7.xtra.model.kraken.stream.Stream
import com.github.exact7.xtra.model.kraken.stream.StreamType

class StreamsDataSource private constructor(
        private val game: String?,
        private val languages: String?,
        private val streamType: StreamType,
        private val api: KrakenApi) : BasePositionalDataSource<Stream>() {

    override fun loadInitial(params: PositionalDataSource.LoadInitialParams, callback: PositionalDataSource.LoadInitialCallback<Stream>) {
        super.loadInitial(params, callback)
        api.getStreams(game, languages, streamType, params.requestedLoadSize, 0)
                .subscribe({ callback.onSuccess(it.streams) }, { callback.onFailure(it, params) })
                .addTo(compositeDisposable)
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams, callback: PositionalDataSource.LoadRangeCallback<Stream>) {
        super.loadRange(params, callback)
        api.getStreams(game, languages, streamType, params.loadSize, params.startPosition)
                .subscribe({ callback.onSuccess(it.streams) }, { callback.onFailure(it, params) })
                .addTo(compositeDisposable)
    }

    class Factory(
            private val game: String?,
            private val languages: String?,
            private val streamType: StreamType,
            private val api: KrakenApi) : BaseDataSourceFactory<Int, Stream, StreamsDataSource>() {

        override fun create(): DataSource<Int, Stream> =
                StreamsDataSource(game, languages, streamType, api).also(sourceLiveData::postValue)
    }
}
