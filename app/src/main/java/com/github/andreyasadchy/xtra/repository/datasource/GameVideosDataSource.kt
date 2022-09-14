package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.GameVideosQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory.apolloClient
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.type.BroadcastType
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class GameVideosDataSource private constructor(
    private val gameId: String?,
    private val gameName: String?,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixPeriod: Period,
    private val helixBroadcastTypes: com.github.andreyasadchy.xtra.model.helix.video.BroadcastType,
    private val helixLanguage: String?,
    private val helixSort: Sort,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlQueryLanguages: List<String>?,
    private val gqlQueryType: BroadcastType?,
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
                    C.GQL_QUERY -> if (helixPeriod == Period.WEEK) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                    C.GQL -> if (helixLanguage.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty() && helixPeriod == Period.WEEK) { api = C.GQL; gqlLoad(params) } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                        C.GQL_QUERY -> if (helixPeriod == Period.WEEK) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                        C.GQL -> if (helixLanguage.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty() && helixPeriod == Period.WEEK) { api = C.GQL; gqlLoad(params) } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                            C.GQL_QUERY -> if (helixPeriod == Period.WEEK) { api = C.GQL_QUERY; gqlQueryLoad(params) } else throw Exception()
                            C.GQL -> if (helixLanguage.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty() && helixPeriod == Period.WEEK) { api = C.GQL; gqlLoad(params) } else throw Exception()
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
        val get = helixApi.getTopVideos(helixClientId, helixToken, gameId, helixPeriod, helixBroadcastTypes, helixLanguage, helixSort, 30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
        val list = mutableListOf<Video>()
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

    private suspend fun gqlQueryLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Video> {
        val get1 = apolloClient(XtraModule(), gqlClientId).query(GameVideosQuery(
            id = Optional.Present(if (!gameId.isNullOrBlank()) gameId else null),
            name = Optional.Present(if (gameId.isNullOrBlank() && !gameName.isNullOrBlank()) gameName else null),
            languages = Optional.Present(gqlQueryLanguages),
            sort = Optional.Present(gqlQuerySort),
            type = Optional.Present(gqlQueryType?.let { listOf(it) }),
            first = Optional.Present(30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/),
            after = Optional.Present(offset)
        )).execute().data?.game?.videos
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
        val get = gqlApi.loadGameVideos(gqlClientId, gameName, gqlType, gqlSort, 30 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
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

    class Factory (
        private val gameId: String?,
        private val gameName: String?,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixPeriod: Period,
        private val helixBroadcastTypes: com.github.andreyasadchy.xtra.model.helix.video.BroadcastType,
        private val helixLanguage: String?,
        private val helixSort: Sort,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlQueryLanguages: List<String>?,
        private val gqlQueryType: BroadcastType?,
        private val gqlQuerySort: VideoSort?,
        private val gqlType: String?,
        private val gqlSort: String?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Video, GameVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                GameVideosDataSource(gameId, gameName, helixClientId, helixToken, helixPeriod, helixBroadcastTypes, helixLanguage, helixSort, helixApi, gqlClientId, gqlQueryLanguages, gqlQueryType, gqlQuerySort, gqlType, gqlSort, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
