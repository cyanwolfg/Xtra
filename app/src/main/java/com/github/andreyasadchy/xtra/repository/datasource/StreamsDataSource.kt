package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.TopStreamsQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class StreamsDataSource private constructor(
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val tags: List<String>?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Stream>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Stream>) {
        loadInitial(params, callback) {
            try {
                when (apiPref.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank() && tags.isNullOrEmpty()) helixInitial(params) else throw Exception()
                    C.GQL_QUERY -> gqlQueryInitial(params)
                    C.GQL -> gqlInitial(params)
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank() && tags.isNullOrEmpty()) helixInitial(params) else throw Exception()
                        C.GQL_QUERY -> gqlQueryInitial(params)
                        C.GQL -> gqlInitial(params)
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank() && tags.isNullOrEmpty()) helixInitial(params) else throw Exception()
                            C.GQL_QUERY -> gqlQueryInitial(params)
                            C.GQL -> gqlInitial(params)
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                }
            }
        }
    }

    private suspend fun helixInitial(params: LoadInitialParams): List<Stream> {
        api = C.HELIX
        val get = helixApi.getTopStreams(helixClientId, helixToken, null, null, params.requestedLoadSize, offset)
        val list = mutableListOf<Stream>()
        get.data?.let { list.addAll(it) }
        val ids = mutableListOf<String>()
        for (i in list) {
            i.user_id?.let { ids.add(it) }
        }
        if (ids.isNotEmpty()) {
            val users = helixApi.getUsers(helixClientId, helixToken, ids).data
            if (users != null) {
                for (i in users) {
                    val items = list.filter { it.user_id == i.id }
                    for (item in items) {
                        item.profileImageURL = i.profile_image_url
                    }
                }
            }
        }
        offset = get.pagination?.cursor
        return list
    }

    private suspend fun gqlQueryInitial(params: LoadInitialParams): List<Stream> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(TopStreamsQuery(
            tags = Optional.Present(tags),
            first = Optional.Present(params.requestedLoadSize),
            after = Optional.Present(offset)
        )).execute().data?.streams
        val get = get1?.edges
        val list = mutableListOf<Stream>()
        if (get != null) {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i?.node?.tags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Stream(
                    id = i?.node?.id,
                    user_id = i?.node?.broadcaster?.id,
                    user_login = i?.node?.broadcaster?.login,
                    user_name = i?.node?.broadcaster?.displayName,
                    game_id = i?.node?.game?.id,
                    game_name = i?.node?.game?.displayName,
                    type = i?.node?.type,
                    title = i?.node?.broadcaster?.broadcastSettings?.title,
                    viewer_count = i?.node?.viewersCount,
                    started_at = i?.node?.createdAt.toString(),
                    thumbnail_url = i?.node?.previewImageURL,
                    profileImageURL = i?.node?.broadcaster?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get.lastOrNull()?.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlInitial(params: LoadInitialParams): List<Stream> {
        api = C.GQL
        val get = gqlApi.loadTopStreams(gqlClientId, tags, params.requestedLoadSize, offset)
        offset = get.cursor
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Stream>) {
        loadRange(params, callback) {
            when (api) {
                C.HELIX -> helixRange(params)
                C.GQL_QUERY -> gqlQueryRange(params)
                C.GQL -> gqlRange(params)
                else -> mutableListOf()
            }
        }
    }

    private suspend fun helixRange(params: LoadRangeParams): List<Stream> {
        val get = helixApi.getTopStreams(helixClientId, helixToken, null, null, params.loadSize, offset)
        val list = mutableListOf<Stream>()
        if (offset != null && offset != "") {
            get.data?.let { list.addAll(it) }
            val ids = mutableListOf<String>()
            for (i in list) {
                i.user_id?.let { ids.add(it) }
            }
            if (ids.isNotEmpty()) {
                val users = helixApi.getUsers(helixClientId, helixToken, ids).data
                if (users != null) {
                    for (i in users) {
                        val items = list.filter { it.user_id == i.id }
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

    private suspend fun gqlQueryRange(params: LoadRangeParams): List<Stream> {
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(TopStreamsQuery(
            tags = Optional.Present(tags),
            first = Optional.Present(params.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.streams
        val get = get1?.edges
        val list = mutableListOf<Stream>()
        if (get != null && nextPage && offset != null && offset != "") {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i?.node?.tags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Stream(
                    id = i?.node?.id,
                    user_id = i?.node?.broadcaster?.id,
                    user_login = i?.node?.broadcaster?.login,
                    user_name = i?.node?.broadcaster?.displayName,
                    game_id = i?.node?.game?.id,
                    game_name = i?.node?.game?.displayName,
                    type = i?.node?.type,
                    title = i?.node?.broadcaster?.broadcastSettings?.title,
                    viewer_count = i?.node?.viewersCount,
                    started_at = i?.node?.createdAt.toString(),
                    thumbnail_url = i?.node?.previewImageURL,
                    profileImageURL = i?.node?.broadcaster?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get.lastOrNull()?.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlRange(params: LoadRangeParams): List<Stream> {
        val get = gqlApi.loadTopStreams(gqlClientId, tags, params.loadSize, offset)
        return if (offset != null && offset != "") {
            offset = get.cursor
            get.data
        } else mutableListOf()
    }

    class Factory(
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val tags: List<String>?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Stream, StreamsDataSource>() {

        override fun create(): DataSource<Int, Stream> =
                StreamsDataSource(helixClientId, helixToken, helixApi, gqlClientId, tags, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
