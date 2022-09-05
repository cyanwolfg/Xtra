package com.github.andreyasadchy.xtra.ui.player.offline

import android.content.Context
import androidx.core.net.toUri
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.ui.player.AudioPlayerService
import com.github.andreyasadchy.xtra.ui.player.PlayerMode
import com.github.andreyasadchy.xtra.ui.player.PlayerViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class OfflinePlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: OfflineRepository) : PlayerViewModel(context) {

    private lateinit var video: OfflineVideo
    val qualities = mutableListOf(context.getString(R.string.source), context.getString(R.string.audio_only))

    init {
        val speed = context.prefs().getFloat(C.PLAYER_SPEED, 1f)
        setSpeed(speed)
    }

    fun setVideo(video: OfflineVideo) {
        val context = XtraApp.INSTANCE.applicationContext
        if (!this::video.isInitialized) {
            this.video = video
            val mediaSourceFactory = if (video.vod) {
                HlsMediaSource.Factory(dataSourceFactory)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
            }
            mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(video.url.toUri()))
            play()
            player.seekTo(if (context.prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) video.lastWatchPosition ?: 0 else 0)
        }
    }

    override fun onResume() {
        isResumed = true
        userLeaveHint = false
        if (playerMode.value == PlayerMode.NORMAL) {
            super.onResume()
            player.seekTo(playbackPosition)
        } else {
            hideAudioNotification()
            if (qualityIndex == 0) {
                changeQuality(qualityIndex)
            }
        }
    }

    override fun onPause() {
        isResumed = false
        if (playerMode.value == PlayerMode.NORMAL) {
            playbackPosition = player.currentPosition
            val context = XtraApp.INSTANCE.applicationContext
            if (!userLeaveHint && !isPaused() && context.prefs().getBoolean(C.PLAYER_LOCK_SCREEN_AUDIO, true)) {
                startAudioOnly(true)
            } else {
                super.onPause()
            }
        } else {
            showAudioNotification()
        }
    }

    override fun changeQuality(index: Int) {
        qualityIndex = index
        if (qualityIndex == 0) {
            playbackPosition = currentPlayer.value!!.currentPosition
            stopBackgroundAudio()
            _currentPlayer.value = player
            play()
            player.seekTo(playbackPosition)
            _playerMode.value = PlayerMode.NORMAL
        } else {
            startAudioOnly()
        }
    }

    fun startAudioOnly(showNotification: Boolean = false) {
        startBackgroundAudio(video.url, video.channelName, video.name, video.channelLogo, true, AudioPlayerService.TYPE_OFFLINE, video.id, showNotification)
        _playerMode.value = PlayerMode.AUDIO_ONLY
    }

    override fun onCleared() {
        if (playerMode.value == PlayerMode.NORMAL) {
            repository.updateVideoPosition(video.id, player.currentPosition)
        } else if (isResumed) {
            stopBackgroundAudio()
        }
        super.onCleared()
    }
}
