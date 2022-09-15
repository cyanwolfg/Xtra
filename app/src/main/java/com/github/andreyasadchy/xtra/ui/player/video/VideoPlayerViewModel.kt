package com.github.andreyasadchy.xtra.ui.player.video

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.VideoDownloadInfo
import com.github.andreyasadchy.xtra.model.VideoPosition
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.Bookmark
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.ui.player.AudioPlayerService
import com.github.andreyasadchy.xtra.ui.player.HlsPlayerViewModel
import com.github.andreyasadchy.xtra.ui.player.PlayerMode
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class VideoPlayerViewModel @Inject constructor(
    context: Application,
    private val playerRepository: PlayerRepository,
    repository: ApiRepository,
    localFollowsChannel: LocalFollowChannelRepository,
    private val bookmarksRepository: BookmarksRepository) : HlsPlayerViewModel(context, repository, localFollowsChannel) {

    private lateinit var video: Video
    val videoInfo: VideoDownloadInfo?
        get() {
            val playlist = (player.currentManifest as? HlsManifest)?.mediaPlaylist ?: return null
            val segments = playlist.segments
            val size = segments.size
            val relativeTimes = ArrayList<Long>(size)
            val durations = ArrayList<Long>(size)
            for (i in 0 until size) {
                val segment = segments[i]
                relativeTimes.add(segment.relativeStartTimeUs / 1000L)
                durations.add(segment.durationUs / 1000L)
            }
            return VideoDownloadInfo(video, helper.urls, relativeTimes, durations, playlist.durationUs / 1000L, playlist.targetDurationUs / 1000L, player.currentPosition)
        }

    override val userId: String?
        get() { return video.user_id }
    override val userLogin: String?
        get() { return video.user_login }
    override val userName: String?
        get() { return video.user_name }
    override val channelLogo: String?
        get() { return video.channelLogo }

    val bookmarkItem = MutableLiveData<Bookmark>()
    val gamesList = MutableLiveData<List<Game>>()
    private var isLoading = false

    init {
        val speed = context.prefs().getFloat(C.PLAYER_SPEED, 1f)
        setSpeed(speed)
    }

    fun loadGamesList(clientId: String?, videoId: String) {
        if (gamesList.value == null && !isLoading) {
            isLoading = true
            viewModelScope.launch {
                try {
                    val get = repository.loadVodGamesGQL(clientId, videoId)
                    if (get != null) {
                        gamesList.postValue(get!!)
                    }
                } catch (e: Exception) {
                    _errors.postValue(e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun seek(position: Long) {
        player.seekTo(position)
    }

    fun setVideo(gqlClientId: String?, gqlToken: String?, video: Video, offset: Double) {
        if (!this::video.isInitialized) {
            this.video = video
            viewModelScope.launch {
                try {
                    val url = playerRepository.loadVideoPlaylistUrl(gqlClientId, gqlToken, video.id)
                    mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(url)
                    play()
                    if (offset > 0) {
                        player.seekTo(offset.toLong())
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun changeQuality(index: Int) {
        previousQuality = qualityIndex
        super.changeQuality(index)
        when {
            index < qualities.lastIndex -> {
                val audioOnly = playerMode.value == PlayerMode.AUDIO_ONLY
                if (audioOnly) {
                    playbackPosition = currentPlayer.value!!.currentPosition
                }
                setVideoQuality(index)
                if (audioOnly) {
                    player.seekTo(playbackPosition)
                }
            }
            else -> startAudioOnly()
        }
    }

    fun startAudioOnly(showNotification: Boolean = false) {
        (player.currentManifest as? HlsManifest)?.let {
            startBackgroundAudio(helper.urls.values.last(), video.user_name, video.title, video.channelLogo, true, AudioPlayerService.TYPE_VIDEO, video.id.toLong(), showNotification)
            _playerMode.value = PlayerMode.AUDIO_ONLY
        }
    }

    override fun onResume() {
        isResumed = true
        userLeaveHint = false
        if (playerMode.value == PlayerMode.NORMAL) {
            super.onResume()
        } else if (playerMode.value == PlayerMode.AUDIO_ONLY) {
            hideAudioNotification()
            if (qualityIndex != qualities.lastIndex) {
                changeQuality(qualityIndex)
            }
        }
        if (playerMode.value != PlayerMode.AUDIO_ONLY) {
            player.seekTo(playbackPosition)
        }
    }

    override fun onPause() {
        isResumed = false
        if (playerMode.value != PlayerMode.AUDIO_ONLY) {
            playbackPosition = player.currentPosition
        }
        val context = getApplication<Application>()
        if (!userLeaveHint && !isPaused() && playerMode.value == PlayerMode.NORMAL && context.prefs().getBoolean(C.PLAYER_LOCK_SCREEN_AUDIO, true)) {
            startAudioOnly(true)
        } else {
            super.onPause()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (error.type == ExoPlaybackException.TYPE_SOURCE &&
            error.sourceException.let { it is HttpDataSource.InvalidResponseCodeException && it.responseCode == 403 }) {
            val context = getApplication<Application>()
            context.toast(R.string.video_subscribers_only)
        } else {
            super.onPlayerError(error)
        }
    }

    override fun onCleared() {
        if (playerMode.value == PlayerMode.NORMAL && this::video.isInitialized) { //TODO
            playerRepository.saveVideoPosition(VideoPosition(video.id.toLong(), player.currentPosition))
        }
        super.onCleared()
    }

    fun checkBookmark() {
        viewModelScope.launch {
            bookmarkItem.postValue(bookmarksRepository.getBookmarkById(video.id))
        }
    }

    fun saveBookmark(context: Context, helixClientId: String?, helixToken: String?, gqlClientId: String?) {
        GlobalScope.launch {
            if (bookmarkItem.value != null) {
                bookmarksRepository.deleteBookmark(context, bookmarkItem.value!!)
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
                val userTypes = video.channelId?.let { repository.loadUserTypes(mutableListOf(it), helixClientId, helixToken, gqlClientId) }?.first()
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
                ))
            }
        }
    }
}