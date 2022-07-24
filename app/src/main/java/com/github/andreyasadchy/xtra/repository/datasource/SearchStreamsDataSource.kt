package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.SearchStreamsQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
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
                    C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                    C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref?.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                        C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    listOf()
                }
            }
        }
    }

    private suspend fun helixLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Stream> {
        val get = helixApi.getChannels(helixClientId, helixToken, query, initialParams?.requestedLoadSize ?: rangeParams?.loadSize, offset, true)
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

    private suspend fun gqlQueryLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Stream> {
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchStreamsQuery(
            query = query,
            first = Optional.Present(initialParams?.requestedLoadSize ?: rangeParams?.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchStreams
        val get = get1?.edges
        val list = mutableListOf<Stream>()
        if (get != null) {
            for (edge in get) {
                edge.node?.let { i ->
                    val tags = mutableListOf<Tag>()
                    i.tags?.forEach { tag ->
                        tags.add(Tag(
                            id = tag.id,
                            name = tag.localizedName
                        ))
                    }
                    list.add(Stream(
                        id = i.id,
                        user_id = i.broadcaster?.id,
                        user_login = i.broadcaster?.login,
                        user_name = i.broadcaster?.displayName,
                        game_id = i.game?.id,
                        game_name = i.game?.displayName,
                        type = i.type,
                        title = i.broadcaster?.broadcastSettings?.title,
                        viewer_count = i.viewersCount,
                        started_at = i.createdAt?.toString(),
                        thumbnail_url = i.previewImageURL,
                        profileImageURL = i.broadcaster?.profileImageURL,
                        tags = tags
                    ))
                }
            }
            offset = get1.edges.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Stream>) {
        loadRange(params, callback) {
            if (!offset.isNullOrBlank()) {
                when (api) {
                    C.HELIX -> helixLoad(rangeParams = params)
                    C.GQL_QUERY -> if (nextPage) gqlQueryLoad(rangeParams = params) else listOf()
                    else -> listOf()
                }
            } else listOf()
        }
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
