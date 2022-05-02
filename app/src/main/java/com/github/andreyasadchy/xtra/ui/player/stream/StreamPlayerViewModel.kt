package com.github.andreyasadchy.xtra.ui.player.stream

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.player.lowlatency.DefaultHlsPlaylistParserFactory
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.player.AudioPlayerService
import com.github.andreyasadchy.xtra.ui.player.HlsPlayerViewModel
import com.github.andreyasadchy.xtra.ui.player.PlayerMode.*
import com.github.andreyasadchy.xtra.util.toast
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistTracker
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class StreamPlayerViewModel @Inject constructor(
    context: Application,
    private val playerRepository: PlayerRepository,
    private val gql: GraphQLRepository,
    repository: TwitchService,
    localFollowsChannel: LocalFollowChannelRepository) : HlsPlayerViewModel(context, repository, localFollowsChannel) {

    private val _stream = MutableLiveData<Stream?>()
    val stream: MutableLiveData<Stream?>
        get() = _stream
    override val userId: String?
        get() { return _stream.value?.user_id }
    override val userLogin: String?
        get() { return _stream.value?.user_login }
    override val userName: String?
        get() { return _stream.value?.user_name }
    override val channelLogo: String?
        get() { return _stream.value?.channelLogo }

    private var useAdBlock = false
    private var randomDeviceId = true
    private var xDeviceId = ""
    private var deviceId = ""
    private var playerType = ""
    private var gqlClientId = ""
    private var minSpeed: Float? = null
    private var maxSpeed: Float? = null
    private var targetOffset: Long? = null

    private val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
        .setAllowChunklessPreparation(true)
        .setPlaylistParserFactory(DefaultHlsPlaylistParserFactory())
        .setPlaylistTrackerFactory(DefaultHlsPlaylistTracker.FACTORY)
        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))

    fun startStream(helixClientId: String?, helixToken: String, stream: Stream, useAdBlock: Boolean, randomDeviceId: Boolean, xDeviceId: String, deviceId: String, playerType: String, gqlClientId: String, minSpeed: String?, maxSpeed: String?, targetOffset: String?) {
        this.useAdBlock = useAdBlock
        this.randomDeviceId = randomDeviceId
        this.xDeviceId = xDeviceId
        this.deviceId = deviceId
        this.playerType = playerType
        this.gqlClientId = gqlClientId
        this.minSpeed = minSpeed?.toFloatOrNull()
        this.maxSpeed = maxSpeed?.toFloatOrNull()
        this.targetOffset = targetOffset?.toLongOrNull()
        if (_stream.value == null) {
            _stream.value = stream
            loadStream(stream)
            viewModelScope.launch {
                while (isActive) {
                    try {
                        val s = when {
                            stream.user_id != null && helixToken.isNotBlank() -> {
                                repository.loadStream(stream.user_id, stream.user_login, helixClientId, helixToken, gqlClientId)
                            }
                            stream.user_login != null -> {
                                Stream(viewer_count = gql.loadViewerCount(gqlClientId, stream.user_login).viewers)
                            }
                            else -> null
                        }
                        _stream.postValue(s)
                        delay(300000L)
                    } catch (e: Exception) {
                        delay(60000L)
                    }
                }
            }
        }
    }

    override fun changeQuality(index: Int) {
        previousQuality = qualityIndex
        super.changeQuality(index)
        when {
            index < qualities.size - 2 -> setVideoQuality(index)
            index < qualities.size - 1 -> startAudioOnly()
            else -> {
                if (playerMode.value == NORMAL) {
                    player.stop()
                } else {
                    stopBackgroundAudio()
                }
                _playerMode.value = DISABLED
            }
        }
    }

    fun startAudioOnly() {
        (player.currentManifest as? HlsManifest)?.let {
            val s = _stream.value!!
            startBackgroundAudio(helper.urls.values.last(), s.user_name, s.title, s.channelLogo, false, AudioPlayerService.TYPE_STREAM, null)
            _playerMode.value = AUDIO_ONLY
        }
    }

    override fun onResume() {
        isResumed = true
        if (playerMode.value == NORMAL) {
            loadStream(stream.value ?: return)
        } else if (playerMode.value == AUDIO_ONLY) {
            hideBackgroundAudio()
            if (qualityIndex < qualities.size - 2) {
                changeQuality(qualityIndex)
            }
        }
    }

    override fun restartPlayer() {
        if (playerMode.value == NORMAL) {
            loadStream(stream.value ?: return)
        } else if (playerMode.value == AUDIO_ONLY) {
            binder?.restartPlayer()
        }
    }

    private fun loadStream(stream: Stream) {
        viewModelScope.launch {
            try {
                val result = stream.user_login?.let { playerRepository.loadStreamPlaylistUrl(gqlClientId, it, playerType, useAdBlock, randomDeviceId, xDeviceId, deviceId) }
                if (result != null) {
                    if (useAdBlock) {
                        if (result.second) {
                            httpDataSourceFactory.setDefaultRequestProperties(hashMapOf("X-Donate-To" to "https://ttv.lol/donate"))
                        } else {
                            val context = getApplication<Application>()
                            context.toast(R.string.adblock_not_working)
                        }
                    }
                    mediaSource = hlsMediaSourceFactory.createMediaSource(
                        MediaItem.Builder().setUri(result.first).setLiveConfiguration(MediaItem.LiveConfiguration.Builder().apply {
                            minSpeed?.let { setMinPlaybackSpeed(it) }
                            maxSpeed?.let { setMaxPlaybackSpeed(it) }
                            targetOffset?.let { setTargetOffsetMs(it) }
                        }.build()).build())
                    play()
                }
            } catch (e: Exception) {
                val context = getApplication<Application>()
                context.toast(R.string.error_stream)
            }
        }
    }

    override fun setSpeed(speed: Float, save: Boolean) {}
}
