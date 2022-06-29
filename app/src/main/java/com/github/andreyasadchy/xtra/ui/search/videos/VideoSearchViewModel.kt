package com.github.andreyasadchy.xtra.ui.search.videos

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.Bookmark
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosViewModel
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class VideoSearchViewModel @Inject constructor(
        context: Application,
        private val repository: TwitchService,
        playerRepository: PlayerRepository,
        private val bookmarksRepository: BookmarksRepository) : BaseVideosViewModel(playerRepository, bookmarksRepository) {

    private val query = MutableLiveData<String>()
    private var gqlClientId = MutableLiveData<String>()
    private var apiPref = MutableLiveData<ArrayList<Pair<Long?, String?>?>>()
    override val result: LiveData<Listing<Video>> = Transformations.map(query) {
        repository.loadSearchVideos(it, gqlClientId.value?.nullIfEmpty(), apiPref.value, viewModelScope)
    }

    fun setQuery(query: String, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>) {
        if (this.gqlClientId.value != gqlClientId) {
            this.gqlClientId.value = gqlClientId
        }
        if (this.apiPref.value != apiPref) {
            this.apiPref.value = apiPref
        }
        if (this.query.value != query) {
            this.query.value = query
        }
    }

    fun saveBookmark(context: Context, video: Video) {
        GlobalScope.launch {
            val item = bookmarksRepository.getBookmarkById(video.id)
            if (item != null) {
                bookmarksRepository.deleteBookmark(context, item)
            } else {
                try {
                    Glide.with(context)
                        .asBitmap()
                        .load(video.thumbnail)
                        .into(object: CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {

                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                DownloadUtils.savePng(context, "thumbnails", video.id, resource)
                            }
                        })
                } catch (e: Exception) {

                }
                try {
                    if (video.channelId != null) {
                        Glide.with(context)
                            .asBitmap()
                            .load(video.channelLogo)
                            .into(object: CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    DownloadUtils.savePng(context, "profile_pics", video.channelId!!, resource)
                                }
                            })
                    }
                } catch (e: Exception) {

                }
                val userTypes = video.channelId?.let { repository.loadUserTypes(mutableListOf(it), null, null, gqlClientId.value) }?.first()
                val downloadedThumbnail = File(context.filesDir.toString() + File.separator + "thumbnails" + File.separator + "${video.id}.png").absolutePath
                val downloadedLogo = File(context.filesDir.toString() + File.separator + "profile_pics" + File.separator + "${video.channelId}.png").absolutePath
                bookmarksRepository.saveBookmark(
                    Bookmark(
                        id = video.id,
                        userId = video.channelId,
                        userLogin = video.channelLogin,
                        userName = video.channelName,
                        userType = userTypes?.type,
                        userBroadcasterType = userTypes?.broadcaster_type,
                        userLogo = downloadedLogo,
                        gameId = video.gameId,
                        gameName = video.gameName,
                        title = video.title,
                        createdAt = video.createdAt,
                        thumbnail = downloadedThumbnail,
                        type = video.type,
                        duration = video.duration,
                    )
                )
            }
        }
    }
}