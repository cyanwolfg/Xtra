package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.SearchGamesQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class SearchGamesDataSource private constructor(
    private val query: String,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>?,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Game>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Game>) {
        loadInitial(params, callback) {
            try {
                when (apiPref?.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) helixInitial(params) else throw Exception()
                    C.GQL_QUERY -> gqlQueryInitial(params)
                    C.GQL -> gqlInitial()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref?.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) helixInitial(params) else throw Exception()
                        C.GQL_QUERY -> gqlQueryInitial(params)
                        C.GQL -> gqlInitial()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref?.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank()) helixInitial(params) else throw Exception()
                            C.GQL_QUERY -> gqlQueryInitial(params)
                            C.GQL -> gqlInitial()
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                }
            }
        }
    }

    private suspend fun helixInitial(params: LoadInitialParams): List<Game> {
        api = C.HELIX
        val get = helixApi.getGames(helixClientId, helixToken, query, params.requestedLoadSize, offset)
        return if (get.data != null) {
            offset = get.pagination?.cursor
            get.data
        } else mutableListOf()
    }

    private suspend fun gqlQueryInitial(params: LoadInitialParams): List<Game> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchGamesQuery(
            query = query,
            first = Optional.Present(params.requestedLoadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchCategories
        val get = get1?.edges
        val list = mutableListOf<Game>()
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
                    list.add(Game(
                        id = i.id,
                        name = i.displayName,
                        box_art_url = i.boxArtURL,
                        viewersCount = i.viewersCount ?: 0, // returns null if 0
                        broadcastersCount = i.broadcastersCount ?: 0, // returns null if 0
                        tags = tags
                    ))
                }
            }
            offset = get1.edges.lastOrNull()?.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlInitial(): List<Game> {
        api = C.GQL
        val get = gqlApi.loadSearchGames(gqlClientId, query, offset)
        offset = get.cursor
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Game>) {
        loadRange(params, callback) {
            when (api) {
                C.HELIX -> helixRange(params)
                C.GQL_QUERY -> gqlQueryRange(params)
                C.GQL -> gqlRange()
                else -> mutableListOf()
            }
        }
    }

    private suspend fun helixRange(params: LoadRangeParams): List<Game> {
        val get = helixApi.getGames(helixClientId, helixToken, query, params.loadSize, offset)
        return if (offset != null && offset != "") {
            if (get.data != null) {
                offset = get.pagination?.cursor
                get.data
            } else mutableListOf()
        } else mutableListOf()
    }

    private suspend fun gqlQueryRange(params: LoadRangeParams): List<Game> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchGamesQuery(
            query = query,
            first = Optional.Present(params.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchCategories
        val get = get1?.edges
        val list = mutableListOf<Game>()
        if (get != null && nextPage && offset != null && offset != "") {
            for (edge in get) {
                edge.node?.let { i ->
                    val tags = mutableListOf<Tag>()
                    i.tags?.forEach { tag ->
                        tags.add(Tag(
                            id = tag.id,
                            name = tag.localizedName
                        ))
                    }
                    list.add(Game(
                        id = i.id,
                        name = i.displayName,
                        box_art_url = i.boxArtURL,
                        viewersCount = i.viewersCount ?: 0, // returns null if 0
                        broadcastersCount = i.broadcastersCount ?: 0, // returns null if 0
                        tags = tags
                    ))
                }
            }
            offset = get1.edges.lastOrNull()?.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlRange(): List<Game> {
        val get = gqlApi.loadSearchGames(gqlClientId, query, offset)
        return if (offset != null && offset != "") {
            offset = get.cursor
            get.data
        } else mutableListOf()
    }

    class Factory(
        private val query: String,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>?,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Game, SearchGamesDataSource>() {

        override fun create(): DataSource<Int, Game> =
                SearchGamesDataSource(query, helixClientId, helixToken, helixApi, gqlClientId, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
