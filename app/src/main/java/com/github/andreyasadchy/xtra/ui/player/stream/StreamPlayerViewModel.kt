package com.github.andreyasadchy.xtra.ui.player.stream

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.player.lowlatency.DefaultHlsPlaylistParserFactory
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.ui.player.AudioPlayerService
import com.github.andreyasadchy.xtra.ui.player.HlsPlayerViewModel
import com.github.andreyasadchy.xtra.ui.player.PlayerMode.*
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.toast
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistTracker
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

var stream_id: String? = null

class StreamPlayerViewModel @Inject constructor(
    context: Application,
    private val playerRepository: PlayerRepository,
    private val gql: GraphQLRepository,
    repository: ApiRepository,
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

    private var gqlClientId: String? = null
    private var gqlToken: String? = null
    private var useAdBlock: Boolean? = true
    private var randomDeviceId: Boolean? = true
    private var xDeviceId: String? = null
    private var deviceId: String? = null
    private var playerType: String? = null
    private var minSpeed: Float? = null
    private var maxSpeed: Float? = null
    private var targetOffset: Long? = null

    private val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
        .setAllowChunklessPreparation(true)
        .setPlaylistParserFactory(DefaultHlsPlaylistParserFactory())
        .setPlaylistTrackerFactory(DefaultHlsPlaylistTracker.FACTORY)
        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))

    fun startStream(user: User, includeToken: Boolean?, helixClientId: String?, gqlClientId: String?, stream: Stream, useAdBlock: Boolean?, randomDeviceId: Boolean?, xDeviceId: String?, deviceId: String?, playerType: String?, minSpeed: String?, maxSpeed: String?, targetOffset: String?, updateStream: Boolean) {
        this.gqlClientId = gqlClientId
        if (includeToken == true) {
            this.gqlToken = user.gqlToken
        }
        this.useAdBlock = useAdBlock
        this.randomDeviceId = randomDeviceId
        this.xDeviceId = xDeviceId
        this.deviceId = deviceId
        this.playerType = playerType
        this.minSpeed = minSpeed?.toFloatOrNull()
        this.maxSpeed = maxSpeed?.toFloatOrNull()
        this.targetOffset = targetOffset?.toLongOrNull()
        if (_stream.value == null) {
            _stream.value = stream
            loadStream(stream)
            if (updateStream) {
                viewModelScope.launch {
                    while (isActive) {
                        try {
                            val s = when {
                                stream.user_id != null -> {
                                    repository.loadStream(stream.user_id, stream.user_login, helixClientId, user.helixToken, gqlClientId) ?:
                                    gql.loadViewerCount(gqlClientId, stream.user_login).let { get ->
                                        _stream.value?.apply {
                                            if (!get.streamId.isNullOrBlank()) {
                                                id = get.streamId
                                            }
                                            viewer_count = get.viewers
                                        }
                                    }
                                }
                                stream.user_login != null -> {
                                    gql.loadViewerCount(gqlClientId, stream.user_login).let { get ->
                                        _stream.value?.apply {
                                            if (!get.streamId.isNullOrBlank()) {
                                                id = get.streamId
                                            }
                                            viewer_count = get.viewers
                                        }
                                    }
                                }
                                else -> null
                            }
                            if (!s?.id.isNullOrBlank()) {
                                stream_id = s?.id
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

    fun startAudioOnly(showNotification: Boolean = false) {
        (player.currentManifest as? HlsManifest)?.let {
            val s = _stream.value!!
            startBackgroundAudio(helper.urls.values.last(), s.user_name, s.title, s.channelLogo, false, AudioPlayerService.TYPE_STREAM, null, showNotification)
            _playerMode.value = AUDIO_ONLY
        }
    }

    override fun onResume() {
        isResumed = true
        userLeaveHint = false
        if (playerMode.value == NORMAL) {
            loadStream(stream.value ?: return)
        } else if (playerMode.value == AUDIO_ONLY) {
            hideAudioNotification()
            if (qualityIndex < qualities.size - 2) {
                changeQuality(qualityIndex)
            }
        }
    }

    override fun onPause() {
        isResumed = false
        val context = getApplication<Application>()
        if (!userLeaveHint && !isPaused() && playerMode.value == NORMAL && context.prefs().getBoolean(C.PLAYER_LOCK_SCREEN_AUDIO, true)) {
            startAudioOnly(true)
        } else {
            super.onPause()
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
                val result = stream.user_login?.let { playerRepository.loadStreamPlaylistUrl(gqlClientId, gqlToken, it, useAdBlock, randomDeviceId, xDeviceId, deviceId, playerType) }
                if (result != null) {
                    if (useAdBlock == true) {
                        if (result.second) {
                            httpDataSourceFactory.defaultRequestProperties.set("X-Donate-To", "https://ttv.lol/donate")
                        } else {
                            val context = getApplication<Application>()
                            context.toast(R.string.adblock_not_working)
                        }
                    }
                    mediaSource = hlsMediaSourceFactory.createMediaSource(result.first)
                    play()
                }
            } catch (e: Exception) {
                val context = getApplication<Application>()
                context.toast(R.string.error_stream)
            }
        }
    }
}
