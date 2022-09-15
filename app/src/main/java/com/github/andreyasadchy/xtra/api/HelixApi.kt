package com.github.andreyasadchy.xtra.api

import com.github.andreyasadchy.xtra.model.chat.CheerEmotesResponse
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearchResponse
import com.github.andreyasadchy.xtra.model.helix.clip.ClipsResponse
import com.github.andreyasadchy.xtra.model.helix.emote.EmoteSetResponse
import com.github.andreyasadchy.xtra.model.helix.follows.FollowResponse
import com.github.andreyasadchy.xtra.model.helix.game.GamesResponse
import com.github.andreyasadchy.xtra.model.helix.stream.StreamsResponse
import com.github.andreyasadchy.xtra.model.helix.user.UsersResponse
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.VideosResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface HelixApi {

    @GET("games")
    suspend fun getGames(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("id") ids: List<String>
    ): GamesResponse

    @GET("games/top")
    suspend fun getTopGames(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("first") limit: Int?,
        @Query("after") offset: String?
    ): GamesResponse

    @GET("streams")
    suspend fun getStreams(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("user_id") ids: List<String>? = null,
        @Query("user_login") logins: List<String>? = null,
        @Query("game_id") gameId: String? = null,
        @Query("language") languages: List<String>? = null,
        @Query("first") limit: Int? = null,
        @Query("after") offset: String? = null
    ): StreamsResponse

    @GET("streams/followed")
    suspend fun getFollowedStreams(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("user_id") userId: String?,
        @Query("first") limit: Int?,
        @Query("after") offset: String?
    ): StreamsResponse

    @GET("clips")
    suspend fun getClips(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("id") ids: List<String>? = null,
        @Query("broadcaster_id") channelId: String? = null,
        @Query("game_id") gameId: String? = null,
        @Query("started_at") started_at: String? = null,
        @Query("ended_at") ended_at: String? = null,
        @Query("first") limit: Int? = null,
        @Query("after") cursor: String? = null
    ): ClipsResponse

    @GET("videos")
    suspend fun getVideos(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("id") ids: List<String>? = null,
        @Query("game_id") gameId: String? = null,
        @Query("user_id") channelId: String? = null,
        @Query("period") period: com.github.andreyasadchy.xtra.model.helix.video.Period? = null,
        @Query("type") broadcastType: BroadcastType? = null,
        @Query("sort") sort: Sort? = null,
        @Query("language") language: String? = null,
        @Query("first") limit: Int? = null,
        @Query("after") offset: String? = null
    ): VideosResponse

    @GET("users")
    suspend fun getUsers(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("id") ids: List<String>? = null,
        @Query("login") logins: List<String>? = null
    ): UsersResponse

    @GET("search/categories")
    suspend fun getGames(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("query") query: String,
        @Query("first") limit: Int?,
        @Query("after") offset: String?
    ): GamesResponse

    @GET("search/channels")
    suspend fun getChannels(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("query") query: String,
        @Query("first") limit: Int?,
        @Query("after") offset: String?,
        @Query("live_only") live: Boolean? = null
    ): ChannelSearchResponse

    @GET("users/follows")
    suspend fun getUserFollows(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("to_id") targetId: String? = null,
        @Query("from_id") userId: String?,
        @Query("first") limit: Int? = null,
        @Query("after") offset: String? = null
    ): FollowResponse

    @GET("chat/emotes/set")
    suspend fun getEmotesFromSet(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("emote_set_id") setIds: List<String>
    ): EmoteSetResponse

    @GET("bits/cheermotes")
    suspend fun getCheerEmotes(
        @Header("Client-ID") clientId: String?,
        @Header("Authorization") token: String?,
        @Query("broadcaster_id") userId: String?
    ): CheerEmotesResponse
}