package com.github.andreyasadchy.xtra.repository.datasource

import androidx.paging.DataSource
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import kotlinx.coroutines.CoroutineScope

class SearchVideosDataSource private constructor(
    private val query: String,
    private val gqlClientId: String?,
    private val gqlApi: GraphQLRepository,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Video>(coroutineScope) {
    private var offset: String? = null

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Video>) {
        loadInitial(params, callback) {
            try {
                gqlInitial()
            } catch (e: Exception) {
                mutableListOf()
            }
        }
    }

    private suspend fun gqlInitial(): List<Video> {
        val get = gqlApi.loadSearchVideos(gqlClientId, query, offset)
        return if (get.data != null) {
            offset = get.cursor
            get.data
        } else mutableListOf()
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Video>) {
        loadRange(params, callback) {
            gqlRange()
        }
    }

    private suspend fun gqlRange(): List<Video> {
        val get = gqlApi.loadSearchVideos(gqlClientId, query, offset)
        return if (offset != null && offset != "") {
            if (get.data != null) {
                offset = get.cursor
                get.data
            } else mutableListOf()
        } else mutableListOf()
    }

    class Factory(
        private val query: String,
        private val gqlClientId: String?,
        private val gqlApi: GraphQLRepository,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Video, SearchVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                SearchVideosDataSource(query, gqlClientId, gqlApi, coroutineScope).also(sourceLiveData::postValue)
    }
}
