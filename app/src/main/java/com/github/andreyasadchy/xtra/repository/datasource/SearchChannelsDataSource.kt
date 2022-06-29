package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.SearchChannelsQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class SearchChannelsDataSource private constructor(
    private val query: String,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>?,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<ChannelSearch>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ChannelSearch>) {
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

    private suspend fun helixInitial(params: LoadInitialParams): List<ChannelSearch> {
        api = C.HELIX
        val get = helixApi.getChannels(helixClientId, helixToken, query, params.requestedLoadSize, offset)
        val list = mutableListOf<ChannelSearch>()
        get.data?.let { list.addAll(it) }
        val ids = mutableListOf<String>()
        for (i in list) {
            i.id?.let { ids.add(it) }
        }
        if (ids.isNotEmpty()) {
            val users = helixApi.getUsersById(helixClientId, helixToken, ids).data
            if (users != null) {
                for (i in users) {
                    val items = list.filter { it.id == i.id }
                    for (item in items) {
                        item.profileImageURL = i.profile_image_url
                    }
                }
            }
        }
        offset = get.pagination?.cursor
        return list
    }

    private suspend fun gqlQueryInitial(params: LoadInitialParams): List<ChannelSearch> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchChannelsQuery(
            query = query,
            first = Optional.Present(params.requestedLoadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchFor?.channels
        val get = get1?.items
        val list = mutableListOf<ChannelSearch>()
        if (get != null) {
            for (i in get) {
                list.add(ChannelSearch(
                    id = i.id,
                    broadcaster_login = i.login,
                    display_name = i.displayName,
                    profileImageURL = i.profileImageURL,
                    followers_count = i.followers?.totalCount,
                    type = i.stream?.type
                ))
            }
            offset = get1.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlInitial(): List<ChannelSearch> {
        api = C.GQL
        val get = gqlApi.loadSearchChannels(gqlClientId, query, offset)
        offset = get.cursor
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ChannelSearch>) {
        loadRange(params, callback) {
            when (api) {
                C.HELIX -> helixRange(params)
                C.GQL_QUERY -> gqlQueryRange(params)
                C.GQL -> gqlRange()
                else -> mutableListOf()
            }
        }
    }

    private suspend fun helixRange(params: LoadRangeParams): List<ChannelSearch> {
        val get = helixApi.getChannels(helixClientId, helixToken, query, params.loadSize, offset)
        val list = mutableListOf<ChannelSearch>()
        if (offset != null && offset != "") {
            get.data?.let { list.addAll(it) }
            val ids = mutableListOf<String>()
            for (i in list) {
                i.id?.let { ids.add(it) }
            }
            if (ids.isNotEmpty()) {
                val users = helixApi.getUsersById(helixClientId, helixToken, ids).data
                if (users != null) {
                    for (i in users) {
                        val items = list.filter { it.id == i.id }
                        for (item in items) {
                            item.profileImageURL = i.profile_image_url
                        }
                    }
                }
            }
            offset = get.pagination?.cursor
        }
        return list
    }

    private suspend fun gqlQueryRange(params: LoadRangeParams): List<ChannelSearch> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchChannelsQuery(
            query = query,
            first = Optional.Present(params.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchFor?.channels
        val get = get1?.items
        val list = mutableListOf<ChannelSearch>()
        if (get != null && nextPage && offset != null && offset != "") {
            for (i in get) {
                list.add(ChannelSearch(
                    id = i.id,
                    broadcaster_login = i.login,
                    display_name = i.displayName,
                    profileImageURL = i.profileImageURL,
                    followers_count = i.followers?.totalCount,
                    type = i.stream?.type
                ))
            }
            offset = get1.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlRange(): List<ChannelSearch> {
        val get = gqlApi.loadSearchChannels(gqlClientId, query, offset)
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
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, ChannelSearch, SearchChannelsDataSource>() {

        override fun create(): DataSource<Int, ChannelSearch> =
                SearchChannelsDataSource(query, helixClientId, helixToken, helixApi, gqlClientId, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
