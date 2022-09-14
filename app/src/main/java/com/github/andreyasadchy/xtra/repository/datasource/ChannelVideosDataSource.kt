package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.UserVideosQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class ChannelVideosDataSource (
    private val channelId: String?,
    private val channelLogin: String?,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixPeriod: Period,
    private val helixBroadcastTypes: BroadcastType,
    private val helixSort: Sort,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlQueryType: com.github.andreyasadchy.xtra.type.BroadcastType?,
    private val gqlQuerySort: VideoSort?,
    private val gqlType: String?,
    private val gqlSort: String?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Video>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Video>) {
        loadInitial(params, callback) {
            try {
                when (apiPref.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                    C.GQL_QUERY -> if (helixPeriod == Period.ALL) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                    C.GQL -> if (helixPeriod == Period.ALL) { api = C.GQL; gqlLoad(params) } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                        C.GQL_QUERY -> if (helixPeriod == Period.ALL) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                        C.GQL -> if (helixPeriod == Period.ALL) { api = C.GQL; gqlLoad(params) } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                            C.GQL_QUERY -> if (helixPeriod == Period.ALL) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                            C.GQL -> if (helixPeriod == Period.ALL) { api = C.GQL; gqlLoad(params) } else throw Exception()
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        listOf()
                    }
                }
            }
        }
    }

    private suspend fun helixLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get = helixApi.getChannelVideos(helixClientId, helixToken, channelId, helixPeriod, helixBroadcastTypes, helixSort, 30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
        return if (get.data != null) {
            offset = get.pagination?.cursor
            get.data
        } else listOf()
    }

    private suspend fun gqlQueryLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(UserVideosQuery(
            id = Optional.Present(channelId),
            sort = Optional.Present(gqlQuerySort),
            types = Optional.Present(gqlQueryType?.let { listOf(it) }),
            first = Optional.Present(30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/),
            after = Optional.Present(offset)
        )).execute().data?.user
        val get = get1?.videos?.edges
        val list = mutableListOf<Video>()
        if (get != null) {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i?.node?.contentTags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Video(
                    id = i?.node?.id ?: "",
                    user_id = channelId,
                    user_login = get1.login,
                    user_name = get1.displayName,
                    gameId = i?.node?.game?.id,
                    gameName = i?.node?.game?.displayName,
                    type = i?.node?.broadcastType?.toString(),
                    title = i?.node?.title,
                    view_count = i?.node?.viewCount,
                    createdAt = i?.node?.createdAt?.toString(),
                    duration = i?.node?.lengthSeconds?.toString(),
                    thumbnail_url = i?.node?.previewThumbnailURL,
                    profileImageURL = get1.profileImageURL,
                    tags = tags
                ))
            }
            offset = get.lastOrNull()?.cursor?.toString()
            nextPage = get1.videos.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get = gqlApi.loadChannelVideos(gqlClientId, channelLogin, gqlType, gqlSort, 30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
        offset = get.cursor
        nextPage = get.hasNextPage ?: true
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Video>) {
        loadRange(params, callback) {
            if (!offset.isNullOrBlank()) {
                when (api) {
                    C.HELIX -> helixLoad(rangeParams = params)
                    C.GQL_QUERY -> if (nextPage) gqlQueryLoad(rangeParams = params) else listOf()
                    C.GQL -> if (nextPage) gqlLoad(rangeParams = params) else listOf()
                    else -> listOf()
                }
            } else listOf()
        }
    }

    class Factory(
        private val channelId: String?,
        private val channelLogin: String?,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixPeriod: Period,
        private val helixBroadcastTypes: BroadcastType,
        private val helixSort: Sort,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlQueryType: com.github.andreyasadchy.xtra.type.BroadcastType?,
        private val gqlQuerySort: VideoSort?,
        private val gqlType: String?,
        private val gqlSort: String?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Video, ChannelVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                ChannelVideosDataSource(channelId, channelLogin, helixClientId, helixToken, helixPeriod, helixBroadcastTypes, helixSort, helixApi, gqlClientId, gqlQueryType, gqlQuerySort, gqlType, gqlSort, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
