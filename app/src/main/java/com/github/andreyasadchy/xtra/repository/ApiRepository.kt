package com.github.andreyasadchy.xtra.repository

import android.util.Base64
import androidx.core.util.Pair
import androidx.paging.PagedList
import com.apollographql.apollo3.api.Optional
import com.github.andreyasadchy.xtra.*
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.api.MiscApi
import com.github.andreyasadchy.xtra.di.XtraModule
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientFactory.apolloClient
import com.github.andreyasadchy.xtra.di.XtraModule_ApolloClientWithTokenFactory.apolloClientWithToken
import com.github.andreyasadchy.xtra.model.chat.CheerEmote
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.model.chat.VideoMessagesResponse
import com.github.andreyasadchy.xtra.model.gql.points.ChannelPointsContextDataResponse
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelViewerList
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.user.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.datasource.*
import com.github.andreyasadchy.xtra.type.ClipsPeriod
import com.github.andreyasadchy.xtra.type.Language
import com.github.andreyasadchy.xtra.type.StreamSort
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.ui.view.chat.animateGifs
import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ApiRepository"

@Singleton
class ApiRepository @Inject constructor(
    private val helix: HelixApi,
    private val gql: GraphQLRepository,
    private val misc: MiscApi,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val localFollowsGame: LocalFollowGameRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository) : TwitchService {

    override fun loadTopGames(helixClientId: String?, helixToken: String?, gqlClientId: String?, tags: List<String>?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = GamesDataSource.Factory(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, tags, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override fun loadTopStreams(helixClientId: String?, helixToken: String?, gqlClientId: String?, tags: List<String>?, apiPref: ArrayList<Pair<Long?, String?>?>, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = StreamsDataSource.Factory(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, tags, gql, apiPref, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadGameStreams(gameId: String?, gameName: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?, gqlQuerySort: StreamSort?, gqlSort: com.github.andreyasadchy.xtra.model.helix.stream.Sort?, tags: List<String>?, apiPref: ArrayList<Pair<Long?, String?>?>, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = GameStreamsDataSource.Factory(gameId, gameName, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, gqlQuerySort, gqlSort, tags, gql, apiPref, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadGameVideos(gameId: String?, gameName: String?, helixClientId: String?, helixToken: String?, helixPeriod: Period, helixBroadcastTypes: BroadcastType, helixLanguage: String?, helixSort: Sort, gqlClientId: String?, gqlQueryLanguages: List<String>?, gqlQueryType: com.github.andreyasadchy.xtra.type.BroadcastType?, gqlQuerySort: VideoSort?, gqlType: String?, gqlSort: String?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = GameVideosDataSource.Factory(gameId, gameName, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helixPeriod, helixBroadcastTypes, helixLanguage, helixSort, helix, gqlClientId, gqlQueryLanguages, gqlQueryType, gqlQuerySort, gqlType, gqlSort, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(30)
            .setInitialLoadSizeHint(30)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadGameClips(gameId: String?, gameName: String?, helixClientId: String?, helixToken: String?, started_at: String?, ended_at: String?, gqlClientId: String?, gqlQueryLanguages: List<Language>?, gqlQueryPeriod: ClipsPeriod?, gqlPeriod: String?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Clip> {
        val factory = GameClipsDataSource.Factory(gameId, gameName, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, started_at, ended_at, helix, gqlClientId, gqlQueryLanguages, gqlQueryPeriod, gqlPeriod, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(20)
            .setInitialLoadSizeHint(20)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadChannelVideos(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?, helixPeriod: Period, helixBroadcastTypes: BroadcastType, helixSort: Sort, gqlClientId: String?, gqlQueryType: com.github.andreyasadchy.xtra.type.BroadcastType?, gqlQuerySort: VideoSort?, gqlType: String?, gqlSort: String?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = ChannelVideosDataSource.Factory(channelId, channelLogin, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helixPeriod, helixBroadcastTypes, helixSort, helix, gqlClientId, gqlQueryType, gqlQuerySort, gqlType, gqlSort, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(30)
            .setInitialLoadSizeHint(30)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadChannelClips(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?, started_at: String?, ended_at: String?, gqlClientId: String?, gqlQueryPeriod: ClipsPeriod?, gqlPeriod: String?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Clip> {
        val factory = ChannelClipsDataSource.Factory(channelId, channelLogin, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, started_at, ended_at, helix, gqlClientId, gqlQueryPeriod, gqlPeriod, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(20)
            .setInitialLoadSizeHint(20)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchChannels(query: String, helixClientId: String?, helixToken: String?, gqlClientId: String?, apiPref: ArrayList<Pair<Long?, String?>?>?, coroutineScope: CoroutineScope): Listing<ChannelSearch> {
        val factory = SearchChannelsDataSource.Factory(query, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(15)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchGames(query: String, helixClientId: String?, helixToken: String?, gqlClientId: String?, apiPref: ArrayList<Pair<Long?, String?>?>?, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = SearchGamesDataSource.Factory(query, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(15)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchVideos(query: String, gqlClientId: String?, apiPref: ArrayList<Pair<Long?, String?>?>?, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = SearchVideosDataSource.Factory(query, gqlClientId, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchStreams(query: String, helixClientId: String?, helixToken: String?, gqlClientId: String?, apiPref: ArrayList<Pair<Long?, String?>?>?, thumbnailsEnabled: Boolean?, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = SearchStreamsDataSource.Factory(query, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, apiPref, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled == true) {
            builder.setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedStreams(userId: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?, gqlToken: String?, apiPref: ArrayList<Pair<Long?, String?>?>, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = FollowedStreamsDataSource.Factory(localFollowsChannel, userId, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gql, apiPref, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(50)
                .setInitialLoadSizeHint(50)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(50)
                .setInitialLoadSizeHint(50)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedVideos(userId: String?, gqlClientId: String?, gqlToken: String?, gqlQueryType: com.github.andreyasadchy.xtra.type.BroadcastType?, gqlQuerySort: VideoSort?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = FollowedVideosDataSource.Factory(userId, gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gqlQueryType, gqlQuerySort, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(50)
            .setInitialLoadSizeHint(50)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedChannels(userId: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?, gqlToken: String?, apiPref: ArrayList<Pair<Long?, String?>?>, sort: com.github.andreyasadchy.xtra.model.helix.follows.Sort, order: Order, coroutineScope: CoroutineScope): Listing<Follow> {
        val factory = FollowedChannelsDataSource.Factory(localFollowsChannel, offlineRepository, bookmarksRepository, userId, helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, helix, gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gql, apiPref, sort, order, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(40)
            .setInitialLoadSizeHint(40)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedGames(userId: String?, gqlClientId: String?, gqlToken: String?, apiPref: ArrayList<Pair<Long?, String?>?>, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = FollowedGamesDataSource.Factory(localFollowsGame, userId, gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gql, apiPref, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(100)
            .setInitialLoadSizeHint(100)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadGameBoxArt(gameId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): String? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), gqlClientId).query(GameBoxArtQuery(Optional.Present(gameId))).execute().data?.game?.boxArtURL
        } catch (e: Exception) {
            helix.getGames(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, mutableListOf(gameId)).data?.firstOrNull()?.boxArt
        }
    }

    override suspend fun loadStream(channelId: String, channelLogin: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            val get = apolloClient(XtraModule(), gqlClientId).query(UsersStreamQuery(Optional.Present(mutableListOf(channelId)))).execute().data?.users?.firstOrNull()
            if (get != null) {
                Stream(id = get.stream?.id, user_id = channelId, user_login = get.login, user_name = get.displayName, game_id = get.stream?.game?.id,
                    game_name = get.stream?.game?.displayName, type = get.stream?.type, title = get.stream?.broadcaster?.broadcastSettings?.title,
                    viewer_count = get.stream?.viewersCount, started_at = get.stream?.createdAt?.toString(), thumbnail_url = get.stream?.previewImageURL,
                    profileImageURL = get.profileImageURL)
            } else null
        } catch (e: Exception) {
            try {
                helix.getStreams(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, mutableListOf(channelId)).data?.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun loadVideo(videoId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): Video? = withContext(Dispatchers.IO) {
        try {
            val get = apolloClient(XtraModule(), gqlClientId).query(VideoQuery(Optional.Present(videoId))).execute().data
            if (get != null) {
                Video(id = videoId, user_id = get.video?.owner?.id, user_login = get.video?.owner?.login, user_name = get.video?.owner?.displayName,
                    profileImageURL = get.video?.owner?.profileImageURL, title = get.video?.title, createdAt = get.video?.createdAt?.toString(), thumbnail_url = get.video?.previewThumbnailURL,
                    type = get.video?.broadcastType?.toString(), duration = get.video?.lengthSeconds?.toString())
            } else null
        } catch (e: Exception) {
            helix.getVideos(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, mutableListOf(videoId)).data?.firstOrNull()
        }
    }

    override suspend fun loadVideos(ids: List<String>, helixClientId: String?, helixToken: String?): List<Video>? = withContext(Dispatchers.IO) {
        helix.getVideos(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, ids).data
    }

    override suspend fun loadClip(clipId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): Clip? = withContext(Dispatchers.IO) {
        try {
            var user: Clip? = null
            try {
                user = gql.loadClipData(gqlClientId, clipId).data
            } catch (e: Exception) {}
            val video = gql.loadClipVideo(gqlClientId, clipId).data
            Clip(id = clipId, broadcaster_id = user?.broadcaster_id, broadcaster_login = user?.broadcaster_login, broadcaster_name = user?.broadcaster_name,
                profileImageURL = user?.profileImageURL, video_id = video?.video_id, duration = video?.duration, videoOffsetSeconds = video?.videoOffsetSeconds ?: user?.videoOffsetSeconds)
        } catch (e: Exception) {
            helix.getClips(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, mutableListOf(clipId)).data?.firstOrNull()
        }
    }

    override suspend fun loadUserChannelPage(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), gqlClientId).query(UserChannelPageQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.let { i ->
                Stream(
                    id = i.stream?.id,
                    user_id = i.id,
                    user_login = i.login,
                    user_name = i.displayName,
                    game_id = i.stream?.game?.id,
                    game_name = i.stream?.game?.displayName,
                    type = i.stream?.type,
                    title = i.stream?.title,
                    viewer_count = i.stream?.viewersCount,
                    started_at = i.stream?.createdAt?.toString(),
                    profileImageURL = i.profileImageURL,
                    channelUser = User(
                        bannerImageURL = i.bannerImageURL,
                        view_count = i.profileViewCount,
                        created_at = i.createdAt?.toString(),
                        followers_count = i.followers?.totalCount,
                        broadcaster_type = when {
                            i.roles?.isPartner == true -> "partner"
                            i.roles?.isAffiliate == true -> "affiliate"
                            else -> null
                        },
                        type = when {
                            i.roles?.isStaff == true -> "staff"
                            i.roles?.isSiteAdmin == true -> "admin"
                            i.roles?.isGlobalMod == true -> "global_mod"
                            else -> null
                        }
                    ),
                    lastBroadcast = i.lastBroadcast?.startedAt?.toString()
                )
            }
        } catch (e: Exception) {
            helix.getStreams(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
        }
    }

    override suspend fun loadUser(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?): User? = withContext(Dispatchers.IO) {
        helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
    }

    override suspend fun loadCheckUser(channelId: String?, channelLogin: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): User? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), gqlClientId).query(UserQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.let { i ->
                User(
                    id = i.id,
                    login = i.login,
                    display_name = i.displayName,
                    profile_image_url = i.profileImageURL
                )
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { listOf(channelId) }, channelLogin?.let { listOf(channelLogin) }).data?.firstOrNull()
        }
    }

    override suspend fun loadUserMessageClicked(channelId: String?, channelLogin: String?, targetId: String?, helixClientId: String?, helixToken: String?, gqlClientId: String?): User? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), gqlClientId).query(UserMessageClickedQuery(Optional.Present(channelId), Optional.Present(channelLogin), Optional.Present(targetId))).execute().data?.user?.let { i ->
                User(
                    id = i.id,
                    login = i.login,
                    display_name = i.displayName,
                    profile_image_url = i.profileImageURL,
                    bannerImageURL = i.bannerImageURL,
                    created_at = i.createdAt?.toString(),
                    followedAt = i.follow?.followedAt?.toString()
                )
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, channelId?.let { mutableListOf(channelId) }, channelLogin?.let { mutableListOf(channelLogin) }).data?.firstOrNull()
        }
    }

    override suspend fun loadUserTypes(ids: List<String>, helixClientId: String?, helixToken: String?, gqlClientId: String?): List<User>? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), gqlClientId).query(UsersTypeQuery(Optional.Present(ids))).execute().data?.users?.let { get ->
                val list = mutableListOf<User>()
                for (i in get) {
                    list.add(User(
                        id = i?.id,
                        broadcaster_type = when {
                            i?.roles?.isPartner == true -> "partner"
                            i?.roles?.isAffiliate == true -> "affiliate"
                            else -> null
                        },
                        type = when {
                            i?.roles?.isStaff == true -> "staff"
                            i?.roles?.isSiteAdmin == true -> "admin"
                            i?.roles?.isGlobalMod == true -> "global_mod"
                            else -> null
                        }
                    ))
                }
                list
            }
        } catch (e: Exception) {
            helix.getUsers(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, ids).data
        }
    }

    override suspend fun loadCheerEmotes(userId: String, helixClientId: String?, helixToken: String?, gqlClientId: String?): List<CheerEmote>? = withContext(Dispatchers.IO) {
        try {
            val emotes = mutableListOf<CheerEmote>()
            val get = apolloClient(XtraModule(), gqlClientId).query(CheerEmotesQuery(Optional.Present(userId), Optional.Present(animateGifs), Optional.Present((when (emoteQuality) {"4" -> 4 "3" -> 3 "2" -> 2 else -> 1}).toDouble()))).execute().data
            if (get?.user?.cheer?.emotes != null) {
                for (i in get.user.cheer.emotes) {
                    if (i?.tiers != null) {
                        for (tier in i.tiers) {
                            i.prefix?.let { tier?.bits?.let { it1 -> tier.images?.first()?.url?.let { it2 -> emotes.add(CheerEmote(name = it, minBits = it1, color = tier.color, type = if (animateGifs) "image/gif" else "image/png", url = it2)) } } }
                        }
                    }
                }
            }
            emotes.ifEmpty { null }
        } catch (e: Exception) {
            helix.getCheerEmotes(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, userId).emotes
        }
    }

    override suspend fun loadUserEmotes(gqlClientId: String?, gqlToken: String?, userId: String, channelId: String?): List<TwitchEmote>? = withContext(Dispatchers.IO) {
        try {
            val emotes = mutableListOf<TwitchEmote>()
            apolloClientWithToken(XtraModule(), gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }).query(UserEmotesQuery(Optional.Present(userId))).execute().data?.user?.emoteSets?.forEach { set ->
                set.emotes?.forEach { emote ->
                    if (emote?.id != null) {
                        emotes.add(TwitchEmote(
                            name = emote.id,
                            setId = emote.setID,
                            ownerId = emote.owner?.id
                        ))
                    }
                }
            }
            emotes
        } catch (e: Exception) {
            try {
                gql.loadUserEmotes(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelId).data
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun loadEmotesFromSet(helixClientId: String?, helixToken: String?, setIds: List<String>): List<TwitchEmote>? = withContext(Dispatchers.IO) {
        helix.getEmotesFromSet(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, setIds).data
    }

    override suspend fun loadUserFollowing(helixClientId: String?, helixToken: String?, userId: String?, channelId: String?, gqlClientId: String?, gqlToken: String?, userLogin: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!gqlToken.isNullOrBlank()) gql.loadFollowingUser(gqlClientId, gqlToken.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userLogin).following else throw Exception()
        } catch (e: Exception) {
            helix.getUserFollows(helixClientId, helixToken?.let { TwitchApiHelper.addTokenPrefixHelix(it) }, userId, channelId).total == 1
        }
    }

    override suspend fun loadGameFollowing(gqlClientId: String?, gqlToken: String?, gameName: String?): Boolean = withContext(Dispatchers.IO) {
        gql.loadFollowingGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameName).following
    }

    override suspend fun loadVideoChatLog(gqlClientId: String?, videoId: String, offsetSeconds: Double): VideoMessagesResponse = withContext(Dispatchers.IO) {
        misc.getVideoChatLog(gqlClientId, videoId, offsetSeconds, 100)
    }

    override suspend fun loadVideoChatAfter(gqlClientId: String?, videoId: String, cursor: String): VideoMessagesResponse = withContext(Dispatchers.IO) {
        misc.getVideoChatLogAfter(gqlClientId, videoId, cursor, 100)
    }

    override suspend fun loadVodGamesGQL(clientId: String?, videoId: String?): List<Game> = withContext(Dispatchers.IO) {
        gql.loadVodGames(clientId, videoId).data
    }

    override suspend fun loadChannelViewerListGQL(clientId: String?, channelLogin: String?): ChannelViewerList = withContext(Dispatchers.IO) {
        gql.loadChannelViewerList(clientId, channelLogin).data
    }

    override suspend fun loadHosting(clientId: String?, channelId: String?, channelLogin: String?): Stream? = withContext(Dispatchers.IO) {
        try {
            apolloClient(XtraModule(), clientId).query(UserHostingQuery(Optional.Present(channelId), Optional.Present(channelLogin))).execute().data?.user?.hosting?.let { get ->
                Stream(
                    user_id = get.id,
                    user_login = get.login,
                    user_name = get.displayName,
                    profileImageURL = get.profileImageURL,
                )
            }
        } catch (e: Exception) {
            if (!channelLogin.isNullOrBlank()) {
                gql.loadChannelHosting(clientId, channelLogin).data
            } else {
                null
            }
        }
    }

    override suspend fun loadChannelPointsContext(gqlClientId: String?, gqlToken: String?, channelLogin: String?): ChannelPointsContextDataResponse = withContext(Dispatchers.IO){
        gql.loadChannelPointsContext(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelLogin)
    }

    override suspend fun loadClaimPoints(gqlClientId: String?, gqlToken: String?, channelId: String?, claimID: String?) = withContext(Dispatchers.IO) {
        gql.loadClaimPoints(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, channelId, claimID)
    }

    override suspend fun loadJoinRaid(gqlClientId: String?, gqlToken: String?, raidId: String?) = withContext(Dispatchers.IO) {
        gql.loadJoinRaid(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, raidId)
    }

    override suspend fun loadMinuteWatched(userId: String?, streamId: String?, channelId: String?, channelLogin: String?) = withContext(Dispatchers.IO) {
        try {
            val pageResponse = channelLogin?.let { misc.getChannelPage(it).string() }
            if (!pageResponse.isNullOrBlank()) {
                val settingsRegex = Regex("(https://static.twitchcdn.net/config/settings.*?js)")
                val settingsUrl = settingsRegex.find(pageResponse)?.value
                val settingsResponse = settingsUrl?.let { misc.getUrl(it).string() }
                if (!settingsResponse.isNullOrBlank()) {
                    val spadeRegex = Regex("\"spade_url\":\"(.*?)\"")
                    val spadeUrl = spadeRegex.find(settingsResponse)?.groups?.get(1)?.value
                    if (!spadeUrl.isNullOrBlank()) {
                        val json = JsonObject().apply {
                            addProperty("event", "minute-watched")
                            add("properties", JsonObject().apply {
                                addProperty("channel_id", channelId)
                                addProperty("broadcast_id", streamId)
                                addProperty("player", "site")
                                addProperty("user_id", userId?.toInt())
                            })
                        }
                        val spadeRequest = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP).toRequestBody()
                        misc.postUrl(spadeUrl, spadeRequest)
                    }
                }
            }
        } catch (e: Exception) {

        }
    }

    override suspend fun followUser(gqlClientId: String?, gqlToken: String?, userId: String?): Boolean = withContext(Dispatchers.IO) {
        gql.loadFollowUser(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userId).error.isNullOrBlank()
    }

    override suspend fun unfollowUser(gqlClientId: String?, gqlToken: String?, userId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadUnfollowUser(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, userId).isJsonNull
    }

    override suspend fun followGame(gqlClientId: String?, gqlToken: String?, gameId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadFollowGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameId).isJsonNull
    }

    override suspend fun unfollowGame(gqlClientId: String?, gqlToken: String?, gameId: String?): Boolean = withContext(Dispatchers.IO) {
        !gql.loadUnfollowGame(gqlClientId, gqlToken?.let { TwitchApiHelper.addTokenPrefixGQL(it) }, gameId).isJsonNull
    }

    override fun loadTagsGQL(clientId: String?, getGameTags: Boolean, gameId: String?, gameName: String?, query: String?, coroutineScope: CoroutineScope): Listing<Tag> {
        val factory = TagsDataSourceGQL.Factory(clientId, getGameTags, gameId, gameName, query, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10000)
            .setInitialLoadSizeHint(10000)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }
}