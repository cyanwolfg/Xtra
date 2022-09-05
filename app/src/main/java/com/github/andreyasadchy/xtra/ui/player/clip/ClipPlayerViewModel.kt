package com.github.andreyasadchy.xtra.ui.player.clip

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import com.github.andreyasadchy.xtra.ui.player.PlayerHelper
import com.github.andreyasadchy.xtra.ui.player.PlayerViewModel
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.shortToast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClipPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val graphQLRepository: GraphQLRepository,
    private val repository: ApiRepository,
    private val localFollowsChannel: LocalFollowChannelRepository) : PlayerViewModel(context), FollowViewModel {

    private lateinit var clip: Clip
    private val factory: ProgressiveMediaSource.Factory = ProgressiveMediaSource.Factory(dataSourceFactory)
    private val prefs = context.prefs()
    private val helper = PlayerHelper()
    val qualities: Map<String, String>
        get() = helper.urls
    val loaded: LiveData<Boolean>
        get() = helper.loaded
    private val _video = MutableLiveData<Video?>()
    val video: MutableLiveData<Video?>
        get() = _video

    override val userId: String?
        get() { return clip.broadcaster_id }
    override val userLogin: String?
        get() { return clip.broadcaster_login }
    override val userName: String?
        get() { return clip.broadcaster_name }
    override val channelLogo: String?
        get() { return clip.channelLogo }
    override lateinit var follow: FollowLiveData

    init {
        val speed = context.prefs().getFloat(C.PLAYER_SPEED, 1f)
        setSpeed(speed)
    }

    override fun changeQuality(index: Int) {
        playbackPosition = player.currentPosition
        val quality = helper.urls.values.elementAt(index)
        play(quality)
        if (prefs.getString(C.PLAYER_DEFAULTQUALITY, "saved") == "saved") {
            prefs.edit { putString(C.PLAYER_QUALITY, helper.urls.keys.elementAt(index)) }
        }
        qualityIndex = index
    }

    override fun onResume() {
        super.onResume()
        player.seekTo(playbackPosition)
    }

    override fun onPause() {
        playbackPosition = player.currentPosition
        super.onPause()
    }

    fun setClip(clip: Clip) {
        if (!this::clip.isInitialized) {
            this.clip = clip
            viewModelScope.launch {
                try {
                    val urls = graphQLRepository.loadClipUrls(prefs.getString(C.GQL_CLIENT_ID, ""), clip.id)
                    val defaultQuality = prefs.getString(C.PLAYER_DEFAULTQUALITY, "saved")
                    val savedQuality = prefs.getString(C.PLAYER_QUALITY, "720p60")
                    val url = when (defaultQuality) {
                        "saved" -> {
                            if (savedQuality == "Auto") {
                                null
                            } else {
                                val item = urls.keys.find { it == savedQuality }
                                if (item != null) {
                                    qualityIndex = urls.keys.indexOf(item)
                                    urls[item]
                                } else null
                            }
                        }
                        else -> {
                            var url: String? = null
                            if (defaultQuality?.toIntOrNull() != null) {
                                for (i in urls.entries.withIndex()) {
                                    val comp = i.value.key.take(4).filter { it.isDigit() }
                                    if (comp != "") {
                                        if (defaultQuality.toInt() >= comp.toInt()) {
                                            qualityIndex = i.index
                                            url = i.value.value
                                            break
                                        }
                                    }
                                }
                            }
                            url
                        }
                    }
                    url.let {
                        if (it != null) {
                            play(it)
                        } else {
                            play(urls.values.first())
                        }
                    }
                    helper.urls = urls
                    helper.loaded.value = true
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun setUser(user: User, helixClientId: String?, gqlClientId: String?, setting: Int) {
        if (!this::follow.isInitialized) {
            follow = FollowLiveData(localFollowsChannel = localFollowsChannel, repository = repository, userId = userId, userLogin = userLogin, userName = userName, channelLogo = channelLogo, user = user, helixClientId = helixClientId, gqlClientId = gqlClientId, setting = setting, viewModelScope = viewModelScope)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val error2 = player.playerError
        if (error2 != null) {
            if (error2.type == ExoPlaybackException.TYPE_UNEXPECTED && error2.unexpectedException is IllegalStateException) {
                val context = XtraApp.INSTANCE.applicationContext
                context.shortToast(R.string.player_error)
                if (qualityIndex < helper.urls.size - 1) {
                    changeQuality(++qualityIndex)
                }
            }
        }
    }

    private fun play(url: String) {
        mediaSource = factory.createMediaSource(MediaItem.fromUri(url.toUri()))
        play()
        player.seekTo(playbackPosition)
    }
}
