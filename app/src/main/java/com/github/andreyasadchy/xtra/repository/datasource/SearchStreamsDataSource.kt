package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class SearchStreamsDataSource private constructor(
    private val query: String,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val apiPref: ArrayList<Pair<Long?, String?>?>?,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Stream>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Stream>) {
        loadInitial(params, callback) {
            try {
                when (apiPref?.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) helixInitial(params) else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                mutableListOf()
            }
        }
    }

    private suspend fun helixInitial(params: LoadInitialParams): List<Stream> {
        api = C.HELIX
        val get = helixApi.getChannels(helixClientId, helixToken, query, params.requestedLoadSize, offset, true)
        val list = mutableListOf<Stream>()
        get.data?.forEach {
            list.add(Stream(
                user_id = it.id,
                user_login = it.broadcaster_login,
                user_name = it.display_name,
                game_id = it.game_id,
                game_name = it.game_name,
                title = it.title,
                started_at = it.started_at,
                profileImageURL = it.thumbnail_url,
            ))
        }
        offset = get.pagination?.cursor
        return list
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Stream>) {
        loadRange(params, callback) {
            when (api) {
                C.HELIX -> helixRange(params)
                else -> mutableListOf()
            }
        }
    }

    private suspend fun helixRange(params: LoadRangeParams): List<Stream> {
        val get = helixApi.getChannels(helixClientId, helixToken, query, params.loadSize, offset, true)
        val list = mutableListOf<Stream>()
        if (offset != null && offset != "") {
            get.data?.forEach {
                list.add(Stream(
                    user_id = it.id,
                    user_login = it.broadcaster_login,
                    user_name = it.display_name,
                    game_id = it.game_id,
                    game_name = it.game_name,
                    title = it.title,
                    started_at = it.started_at,
                    profileImageURL = it.thumbnail_url,
                ))
            }
            offset = get.pagination?.cursor
        }
        return list
    }

    class Factory(
        private val query: String,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val apiPref: ArrayList<Pair<Long?, String?>?>?,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Stream, SearchStreamsDataSource>() {

        override fun create(): DataSource<Int, Stream> =
                SearchStreamsDataSource(query, helixClientId, helixToken, helixApi, gqlClientId, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}