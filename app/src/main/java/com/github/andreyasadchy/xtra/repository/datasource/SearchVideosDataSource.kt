package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.SearchVideosQuery
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class SearchVideosDataSource private constructor(
    private val query: String,
    private val gqlClientId: String?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>?,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Video>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Video>) {
        loadInitial(params, callback) {
            try {
                when (apiPref?.elementAt(0)?.second) {
                    C.GQL_QUERY -> gqlQueryInitial(params)
                    C.GQL -> gqlInitial()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref?.elementAt(1)?.second) {
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

    private suspend fun gqlQueryInitial(params: LoadInitialParams): List<Video> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchVideosQuery(
            query = query,
            first = Optional.Present(params.requestedLoadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchFor?.videos
        val get = get1?.items
        val list = mutableListOf<Video>()
        if (get != null) {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i.contentTags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Video(
                    id = i.id ?: "",
                    user_id = i.owner?.id,
                    user_login = i.owner?.login,
                    user_name = i.owner?.displayName,
                    type = i.broadcastType.toString(),
                    title = i.title,
                    view_count = i.viewCount,
                    createdAt = i.createdAt.toString(),
                    duration = i.lengthSeconds.toString(),
                    thumbnail_url = i.previewThumbnailURL,
                    gameId = i.game?.id,
                    gameName = i.game?.displayName,
                    profileImageURL = i.owner?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get1.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlInitial(): List<Video> {
        val get = gqlApi.loadSearchVideos(gqlClientId, query, offset)
        offset = get.cursor
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Video>) {
        loadRange(params, callback) {
            when (api) {
                C.GQL_QUERY -> gqlQueryRange(params)
                C.GQL -> gqlRange()
                else -> mutableListOf()
            }
        }
    }

    private suspend fun gqlQueryRange(params: LoadRangeParams): List<Video> {
        api = C.GQL_QUERY
        val get1 = XtraModule_ApolloClientFactory.apolloClient(XtraModule(), gqlClientId).query(SearchVideosQuery(
            query = query,
            first = Optional.Present(params.loadSize),
            after = Optional.Present(offset)
        )).execute().data?.searchFor?.videos
        val get = get1?.items
        val list = mutableListOf<Video>()
        if (get != null && nextPage && offset != null && offset != "") {
            for (i in get) {
                val tags = mutableListOf<Tag>()
                i.contentTags?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.id,
                        name = tag.localizedName
                    ))
                }
                list.add(Video(
                    id = i.id ?: "",
                    user_id = i.owner?.id,
                    user_login = i.owner?.login,
                    user_name = i.owner?.displayName,
                    type = i.broadcastType.toString(),
                    title = i.title,
                    view_count = i.viewCount,
                    createdAt = i.createdAt.toString(),
                    duration = i.lengthSeconds.toString(),
                    thumbnail_url = i.previewThumbnailURL,
                    gameId = i.game?.id,
                    gameName = i.game?.displayName,
                    profileImageURL = i.owner?.profileImageURL,
                    tags = tags
                ))
            }
            offset = get1.cursor.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlRange(): List<Video> {
        val get = gqlApi.loadSearchVideos(gqlClientId, query, offset)
        return if (offset != null && offset != "") {
            offset = get.cursor
            get.data
        } else mutableListOf()
    }

    class Factory(
        private val query: String,
        private val gqlClientId: String?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>?,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Video, SearchVideosDataSource>() {

        override fun create(): DataSource<Int, Video> =
                SearchVideosDataSource(query, gqlClientId, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
