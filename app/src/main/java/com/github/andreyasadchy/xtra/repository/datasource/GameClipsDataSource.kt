package com.github.andreyasadchy.xtra.repository.datasource

import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.GameClipsQuery
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory.apolloClient
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.type.ClipsPeriod
import com.github.andreyasadchy.xtra.type.Language
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.CoroutineScope

class GameClipsDataSource(
    private val gameId: String?,
    private val gameName: String?,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val started_at: String?,
    private val ended_at: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlQueryLanguages: List<Language>?,
    private val gqlQueryPeriod: ClipsPeriod?,
    private val gqlPeriod: String?,
    private val gqlApi: GraphQLRepository,
    private val apiPref: ArrayList<Pair<Long?, String?>?>,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Clip>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Clip>) {
        loadInitial(params, callback) {
            try {
                when (apiPref.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                    C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                    C.GQL -> if (gqlQueryLanguages.isNullOrEmpty()) { api = C.GQL; gqlLoad(params) } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                        C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                        C.GQL -> if (gqlQueryLanguages.isNullOrEmpty()) { api = C.GQL; gqlLoad(params) } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank() && gqlQueryLanguages.isNullOrEmpty()) { api = C.HELIX; helixLoad(params) } else throw Exception()
                            C.GQL_QUERY -> { api = C.GQL_QUERY; gqlQueryLoad(params) }
                            C.GQL -> if (gqlQueryLanguages.isNullOrEmpty()) { api = C.GQL; gqlLoad(params) } else throw Exception()
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        listOf()
                    }
                }
            }
        }
    }

    private suspend fun helixLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Clip> {
        val get = helixApi.getClips(clientId = helixClientId, token = helixToken, gameId = gameId, started_at = started_at, ended_at = ended_at, limit = 20 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, cursor = offset)
        val list = mutableListOf<Clip>()
        get.data?.let { list.addAll(it) }
        val userIds = mutableListOf<String>()
        for (i in list) {
            i.broadcaster_id?.let { userIds.add(it) }
        }
        if (userIds.isNotEmpty()) {
            val users = helixApi.getUsers(helixClientId, helixToken, userIds).data
            if (users != null) {
                for (i in users) {
                    val items = list.filter { it.broadcaster_id == i.id }
                    for (item in items) {
                        item.broadcaster_login = i.login
                        item.profileImageURL = i.profile_image_url
                    }
                }
            }
        }
        offset = get.pagination?.cursor
        return list
    }

    private suspend fun gqlQueryLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Clip> {
        val get1 = apolloClient(XtraModule(), gqlClientId).query(GameClipsQuery(
            id = Optional.Present(if (!gameId.isNullOrBlank()) gameId else null),
            name = Optional.Present(if (gameId.isNullOrBlank() && !gameName.isNullOrBlank()) gameName else null),
            languages = Optional.Present(gqlQueryLanguages),
            sort = Optional.Present(gqlQueryPeriod),
            first = Optional.Present(20 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/),
            after = Optional.Present(offset)
        )).execute().data?.game?.clips
        val get = get1?.edges
        val list = mutableListOf<Clip>()
        if (get != null) {
            for (i in get) {
                list.add(Clip(
                    id = i?.node?.slug ?: "",
                    broadcaster_id = i?.node?.broadcaster?.id,
                    broadcaster_login = i?.node?.broadcaster?.login,
                    broadcaster_name = i?.node?.broadcaster?.displayName,
                    video_id = i?.node?.video?.id,
                    videoOffsetSeconds = i?.node?.videoOffsetSeconds,
                    title = i?.node?.title,
                    view_count = i?.node?.viewCount,
                    created_at = i?.node?.createdAt?.toString(),
                    duration = i?.node?.durationSeconds?.toDouble(),
                    thumbnail_url = i?.node?.thumbnailURL,
                    profileImageURL = i?.node?.broadcaster?.profileImageURL,
                ))
            }
            offset = get.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlLoad(initialParams: LoadInitialParams? = null, rangeParams: LoadRangeParams? = null): List<Clip> {
        val get = gqlApi.loadGameClips(gqlClientId, gameName, gqlPeriod, 20 /*initialParams?.requestedLoadSize ?: rangeParams?.loadSize*/, offset)
        offset = get.cursor
        nextPage = get.hasNextPage ?: true
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Clip>) {
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
        private val gameId: String?,
        private val gameName: String?,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val started_at: String?,
        private val ended_at: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlQueryLanguages: List<Language>?,
        private val gqlQueryPeriod: ClipsPeriod?,
        private val gqlPeriod: String?,
        private val gqlApi: GraphQLRepository,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Clip, GameClipsDataSource>() {

        override fun create(): DataSource<Int, Clip> =
                GameClipsDataSource(gameId, gameName, helixClientId, helixToken, started_at, ended_at, helixApi, gqlClientId, gqlQueryLanguages, gqlQueryPeriod, gqlPeriod, gqlApi, apiPref, coroutineScope).also(sourceLiveData::postValue)
    }
}
