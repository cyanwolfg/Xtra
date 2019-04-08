package com.github.exact7.xtra.ui.player.video

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.exact7.xtra.model.VideoDownloadInfo
import com.github.exact7.xtra.model.kraken.video.Video
import com.github.exact7.xtra.repository.PlayerRepository
import com.github.exact7.xtra.repository.TwitchService
import com.github.exact7.xtra.ui.player.ChatReplayManager
import com.github.exact7.xtra.ui.player.HlsPlayerViewModel
import com.github.exact7.xtra.ui.player.PlayerMode
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class VideoPlayerViewModel @Inject constructor(
        context: Application,
        private val playerRepository: PlayerRepository,
        repository: TwitchService) : HlsPlayerViewModel(context, repository) {

    private val _video = MutableLiveData<Video>()
    val video: LiveData<Video>
        get() = _video
    private var playbackProgress: Long = 0
    private lateinit var playlist: HlsMediaPlaylist
    val videoInfo: VideoDownloadInfo
        get() = VideoDownloadInfo(_video.value!!, helper.urls!!, playlist.segments.map { it.relativeStartTimeUs / 1000000L }, playlist.durationUs / 1000000L, playlist.targetDurationUs / 1000000L, player.currentPosition / 1000L)
    override val channelInfo: Pair<String, String>
        get()  {
            val v = video.value!!
            return v.channel.id to v.channel.displayName
        }
    private lateinit var chatReplayManager: ChatReplayManager

    fun setVideo(video: Video) {
        if (_video.value != video) {
            _video.value = video
            playerRepository.fetchVideoPlaylist(video.id)
                    .map { Uri.parse(it.raw().request().url().toString()) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(it)
                        play()
                    }, {

                    })
                    .addTo(compositeDisposable)
            chatReplayManager = ChatReplayManager(repository, video.id, 0.0, player, this::onMessage, this::clearMessages)
            initChat(playerRepository, video.channel.id, video.channelName)
        }
    }

    override fun changeQuality(index: Int) {
        super.changeQuality(index)
        when {
            index < qualities.lastIndex -> updateQuality(index)
            else -> changePlayerMode(PlayerMode.AUDIO_ONLY)
        }
    }

    override fun onResume() {
        super.onResume()
        player.seekTo(playbackProgress)
    }

    override fun onPause() {
        super.onPause()
        playbackProgress = player.currentPosition
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
        super.onTimelineChanged(timeline, manifest, reason)
        if (!this::playlist.isInitialized && manifest is HlsManifest) {
            playlist = manifest.mediaPlaylist
        }
    }

    override fun onCleared() {
        chatReplayManager.stop()
        super.onCleared()
    }
}
