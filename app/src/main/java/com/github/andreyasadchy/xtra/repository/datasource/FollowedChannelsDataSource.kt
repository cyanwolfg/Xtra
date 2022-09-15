package com.github.andreyasadchy.xtra.repository.datasource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.util.Pair
import androidx.paging.DataSource
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.FollowedUsersQuery
import com.github.andreyasadchy.xtra.UsersLastBroadcastQuery
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class FollowedChannelsDataSource(
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val userId: String?,
    private val helixClientId: String?,
    private val helixToken: String?,
    private val helixApi: HelixApi,
    private val gqlClientId: String?,
    private val gqlToken: String?,
    private val gqlApi: GraphQLRepository,
    private val apolloClient: ApolloClient,
    private val apiPref: ArrayList<Pair<Long?, String?>?>,
    private val sort: Sort,
    private val order: Order,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Follow>(coroutineScope) {
    private var api: String? = null
    private var offset: String? = null
    private var nextPage: Boolean = true

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Follow>) {
        loadInitial(params, callback) {
            val list = mutableListOf<Follow>()
            for (i in localFollowsChannel.loadFollows()) {
                list.add(Follow(to_id = i.user_id, to_login = i.user_login, to_name = i.user_name, profileImageURL = i.channelLogo, followLocal = true))
            }
            val remote = try {
                when (apiPref.elementAt(0)?.second) {
                    C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                    C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                    C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                    else -> throw Exception()
                }
            } catch (e: Exception) {
                try {
                    when (apiPref.elementAt(1)?.second) {
                        C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                        C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                        C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    try {
                        when (apiPref.elementAt(2)?.second) {
                            C.HELIX -> if (!helixToken.isNullOrBlank()) { api = C.HELIX; helixLoad() } else throw Exception()
                            C.GQL_QUERY -> if (!gqlToken.isNullOrBlank()) { api = C.GQL_QUERY; gqlQueryLoad() } else throw Exception()
                            C.GQL -> if (!gqlToken.isNullOrBlank()) { api = C.GQL; gqlLoad() } else throw Exception()
                            else -> throw Exception()
                        }
                    } catch (e: Exception) {
                        listOf()
                    }
                }
            }
            if (remote.isNotEmpty()) {
                for (i in remote) {
                    val item = list.find { it.to_id == i.to_id }
                    if (item == null) {
                        i.followTwitch = true
                        list.add(i)
                    } else {
                        item.followTwitch = true
                        item.followed_at = i.followed_at
                        item.lastBroadcast = i.lastBroadcast
                    }
                }
            }
            val allIds = mutableListOf<String>()
            for (i in list) {
                if (i.profileImageURL == null || i.profileImageURL?.contains("image_manager_disk_cache") == true || i.lastBroadcast == null) {
                    i.to_id?.let { allIds.add(it) }
                }
            }
            if (allIds.isNotEmpty()) {
                for (ids in allIds.chunked(100)) {
                    val get = apolloClient.newBuilder().apply { gqlClientId?.let { addHttpHeader("Client-ID", it) } }.build().query(UsersLastBroadcastQuery(Optional.Present(ids))).execute().data?.users
                    if (get != null) {
                        for (user in get) {
                            val item = list.find { it.to_id == user?.id }
                            if (item != null) {
                                if (item.followLocal) {
                                    if (item.profileImageURL == null || item.profileImageURL?.contains("image_manager_disk_cache") == true) {
                                        val appContext = XtraApp.INSTANCE.applicationContext
                                        item.to_id?.let { id -> user?.profileImageURL?.let { profileImageURL -> updateLocalUser(appContext, id, profileImageURL) } }
                                    }
                                } else {
                                    if (item.profileImageURL == null) {
                                        item.profileImageURL = user?.profileImageURL
                                    }
                                }
                                item.lastBroadcast = user?.lastBroadcast?.startedAt?.toString()
                            }
                        }
                    }
                }
            }
            if (order == Order.ASC) {
                when (sort) {
                    Sort.FOLLOWED_AT -> list.sortedWith(compareBy(nullsLast()) { it.followed_at })
                    Sort.LAST_BROADCAST -> list.sortedWith(compareBy(nullsLast()) { it.lastBroadcast })
                    else -> list.sortedWith(compareBy(nullsLast()) { it.to_login })
                }
            } else {
                when (sort) {
                    Sort.FOLLOWED_AT -> list.sortedWith(compareByDescending(nullsFirst()) { it.followed_at })
                    Sort.LAST_BROADCAST -> list.sortedWith(compareByDescending(nullsFirst()) { it.lastBroadcast })
                    else -> list.sortedWith(compareByDescending(nullsFirst()) { it.to_login })
                }
            }
        }
    }

    private suspend fun helixLoad(): List<Follow> {
        val get = helixApi.getUserFollows(
            clientId = helixClientId,
            token = helixToken,
            userId = userId,
            limit = 100,
            offset = offset
        )
        return if (get.data != null) {
            offset = get.pagination?.cursor
            get.data
        } else listOf()
    }

    private suspend fun gqlQueryLoad(): List<Follow> {
        val get1 = apolloClient.newBuilder().apply {
            gqlClientId?.let { addHttpHeader("Client-ID", it) }
            gqlToken?.let { addHttpHeader("Authorization", it) }
        }.build().query(FollowedUsersQuery(
            id = Optional.Present(userId),
            first = Optional.Present(100),
            after = Optional.Present(offset)
        )).execute().data?.user?.follows
        val get = get1?.edges
        val list = mutableListOf<Follow>()
        if (get != null) {
            for (i in get) {
                list.add(Follow(
                    to_id = i?.node?.id,
                    to_login = i?.node?.login,
                    to_name = i?.node?.displayName,
                    followed_at = i?.followedAt?.toString(),
                    lastBroadcast = i?.node?.lastBroadcast?.startedAt?.toString(),
                    profileImageURL = i?.node?.profileImageURL,
                ))
            }
            offset = get.lastOrNull()?.cursor?.toString()
            nextPage = get1.pageInfo?.hasNextPage ?: true
        }
        return list
    }

    private suspend fun gqlLoad(): List<Follow> {
        val get = gqlApi.loadFollowedChannels(gqlClientId, gqlToken, 100, offset)
        offset = get.cursor
        nextPage = get.hasNextPage ?: true
        return get.data
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Follow>) {
        loadRange(params, callback) {
            val list = if (!offset.isNullOrBlank()) {
                when (api) {
                    C.HELIX -> helixLoad()
                    C.GQL_QUERY -> if (nextPage) gqlQueryLoad() else listOf()
                    C.GQL -> if (nextPage) gqlLoad() else listOf()
                    else -> listOf()
                }
            } else listOf()
            val allIds = mutableListOf<String>()
            for (i in list) {
                if (i.profileImageURL == null || i.lastBroadcast == null) {
                    i.to_id?.let { allIds.add(it) }
                }
            }
            if (allIds.isNotEmpty()) {
                for (ids in allIds.chunked(100)) {
                    val get = apolloClient.newBuilder().apply { gqlClientId?.let { addHttpHeader("Client-ID", it) } }.build().query(UsersLastBroadcastQuery(Optional.Present(ids))).execute().data?.users
                    if (get != null) {
                        for (user in get) {
                            val item = list.find { it.to_id == user?.id }
                            if (item != null) {
                                if (item.followLocal) {
                                    if (item.profileImageURL == null || item.profileImageURL?.contains("image_manager_disk_cache") == true) {
                                        val appContext = XtraApp.INSTANCE.applicationContext
                                        item.to_id?.let { id -> user?.profileImageURL?.let { profileImageURL -> updateLocalUser(appContext, id, profileImageURL) } }
                                    }
                                } else {
                                    if (item.profileImageURL == null) {
                                        item.profileImageURL = user?.profileImageURL
                                    }
                                }
                                item.lastBroadcast = user?.lastBroadcast?.startedAt?.toString()
                            }
                        }
                    }
                }
            }
            list
        }
    }

    private fun updateLocalUser(context: Context, userId: String, profileImageURL: String) {
        GlobalScope.launch {
            try {
                try {
                    Glide.with(context)
                        .asBitmap()
                        .load(TwitchApiHelper.getTemplateUrl(profileImageURL, "profileimage"))
                        .into(object: CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {

                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                DownloadUtils.savePng(context, "profile_pics", userId, resource)
                            }
                        })
                } catch (e: Exception) {

                }
                val downloadedLogo = File(context.filesDir.toString() + File.separator + "profile_pics" + File.separator + "${userId}.png").absolutePath
                localFollowsChannel.getFollowById(userId)?.let { localFollowsChannel.updateFollow(it.apply {
                    channelLogo = downloadedLogo }) }
                for (i in offlineRepository.getVideosByUserId(userId.toInt())) {
                    offlineRepository.updateVideo(i.apply {
                        channelLogo = downloadedLogo })
                }
                for (i in bookmarksRepository.getBookmarksByUserId(userId)) {
                    bookmarksRepository.updateBookmark(i.apply {
                        userLogo = downloadedLogo })
                }
            } catch (e: Exception) {

            }
        }
    }

    class Factory(
        private val localFollowsChannel: LocalFollowChannelRepository,
        private val offlineRepository: OfflineRepository,
        private val bookmarksRepository: BookmarksRepository,
        private val userId: String?,
        private val helixClientId: String?,
        private val helixToken: String?,
        private val helixApi: HelixApi,
        private val gqlClientId: String?,
        private val gqlToken: String?,
        private val gqlApi: GraphQLRepository,
        private val apolloClient: ApolloClient,
        private val apiPref: ArrayList<Pair<Long?, String?>?>,
        private val sort: Sort,
        private val order: Order,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Follow, FollowedChannelsDataSource>() {

        override fun create(): DataSource<Int, Follow> =
                FollowedChannelsDataSource(localFollowsChannel, offlineRepository, bookmarksRepository, userId, helixClientId, helixToken, helixApi, gqlClientId, gqlToken, gqlApi, apolloClient, apiPref, sort, order, coroutineScope).also(sourceLiveData::postValue)
    }
}
