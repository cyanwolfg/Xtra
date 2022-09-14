package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.FollowedVideosQuery
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientWithTokenFactory.apolloClientWithToken
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.type.BroadcastType
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class FollowedVideosDataSource(
    private val userId: String?,
    private val gqlClientId: String?,
    private val gqlToken: String?,
    private val gqlQueryType: BroadcastType?,
    private val gqlQuerySort: VideoSort?,
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
                    C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                    C.GQL -> if (!gqlToken.isNullOrBlank() && gqlQueryType == BroadcastType.ARCHIVE && gqlQuerySort == VideoSort.TIME) { api = C.GQL; gqlLoad(params) } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                        C.GQL -> if (!gqlToken.isNullOrBlank() && gqlQueryType == BroadcastType.ARCHIVE && gqlQuerySort == VideoSort.TIME) { api = C.GQL; gqlLoad(params) } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    listOf()
                }
            }
        }
    }

    private suspend fun gqlQueryLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get1 = apolloClientWithToken(XtraModule(), gqlClientId, gqlToken).query(FollowedVideosQuery(
            id = Optional.Present(userId),
            sort = Optional.Present(gqlQuerySort),
            type = Optional.Present(gqlQueryType?.let { listOf(it) }),
            first = Optional.Present(50 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/),
            after = Optional.Present(offset)
        )).execute().data?.user?.followedVideos
        val get = get1?.edges
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
                    user_id = i?.node?.owner?.id,
                    user_login = i?.node?.owner?.login,
                    user_name = i?.node?.owner?.displayName,
                    gameId = i?.node?.game?.id,
                    gameName = i?.node?.game?.displayName,
                    type = i?.node?.broadcastType?.toString(),
                    title = i?.node?.title,
                    view_count = i?.node?.viewCount,
                    createdAt = i?.node?.createdAt?.toString(),
                    duration = i?.node?.lengthSeconds?.toString(),
                    thumbnail_url = i?.node?.previewThumbnailURL,
                    profileImageURL = i?.node?.owner?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get = gqlApi.loadFollowedVideos(gqlClientId, gqlToken, 50 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
        offset = get.cursor
        nextPage = get.hasNextPage ?: true
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Video>) {
        loadRange(params, callback) {
            if (!offset.isNullOrBlank()) {
                when (api) {
                    C.GQL_QUERY -> if (nextPage) gqlQueryLoad(rangeParams = params) else listOf()
                    C.GQL -> if (nextPage) gqlLoad(rangeParams = params) else listOf()
                    else -> listOf()
                }
            } else listOf()
        }
    }

    class Factory(
        private val userId: String?,
        private val gqlClientId: String?,
        private val gqlToken: String?,
        private val gqlQueryType: BroadcastType?,
        private val gqlQuerySort: VideoSort?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Video, FollowedVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                FollowedVideosDataSource(userId, gqlClientId, gqlToken, gqlQueryType, gqlQuerySort, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
