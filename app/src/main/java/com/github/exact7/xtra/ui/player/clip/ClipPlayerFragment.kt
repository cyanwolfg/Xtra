package com.github.exact7.xtra.ui.player.clip

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.Observer
import com.github.exact7.xtra.R
import com.github.exact7.xtra.model.kraken.Channel
import com.github.exact7.xtra.model.kraken.clip.Clip
import com.github.exact7.xtra.ui.chat.ChatFragment
import com.github.exact7.xtra.ui.chat.ChatReplayPlayerFragment
import com.github.exact7.xtra.ui.common.RadioButtonDialogFragment
import com.github.exact7.xtra.ui.download.ClipDownloadDialog
import com.github.exact7.xtra.ui.download.HasDownloadDialog
import com.github.exact7.xtra.ui.player.BasePlayerFragment
import com.github.exact7.xtra.util.C
import com.github.exact7.xtra.util.DownloadUtils
import com.github.exact7.xtra.util.FragmentUtils
import com.github.exact7.xtra.util.TwitchApiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ClipPlayerFragment : BasePlayerFragment(), RadioButtonDialogFragment.OnSortOptionChanged, HasDownloadDialog, ChatReplayPlayerFragment {
//    override fun play(obj: Parcelable) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override lateinit var viewModel: ClipPlayerViewModel
    private lateinit var clip: Clip
    override val channel: Channel
        get() = clip.broadcaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clip = requireArguments().getParcelable(C.CLIP)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player_clip, container, false)
    }

    override fun initialize() {
        if (childFragmentManager.findFragmentById(R.id.chatFragmentContainer) == null) {
            var videoId: String? = null
            var startTime: Double? = null
            clip.vod?.let {
                videoId = "v${it.id}"
                startTime = TwitchApiHelper.parseClipOffset(it.url)
            }
            childFragmentManager.beginTransaction().replace(R.id.chatFragmentContainer, ChatFragment.newInstance(channel, videoId, startTime)).commit()
        }
        viewModel = createViewModel(ClipPlayerViewModel::class.java)
        viewModel.setClip(clip)
        initializeViewModel(viewModel)
        val settings = requireView().findViewById<ImageButton>(R.id.settings)
        val download = requireView().findViewById<ImageButton>(R.id.download)
        viewModel.loaded.observe(this, Observer {
            settings.isEnabled = true
            download.isEnabled = true
            settings.setColorFilter(Color.WHITE)
            download.setColorFilter(Color.WHITE)
        })
        settings.setOnClickListener { FragmentUtils.showRadioButtonDialogFragment(childFragmentManager, viewModel.qualities.keys, viewModel.selectedQualityIndex) }
        download.setOnClickListener { showDownloadDialog() }
    }

    override fun onChange(index: Int, text: CharSequence, tag: Int?) {
        viewModel.changeQuality(index)
    }

    override fun showDownloadDialog() {
        if (DownloadUtils.hasStoragePermission(requireActivity())) {
            ClipDownloadDialog.newInstance(viewModel.clip.value!!, viewModel.qualities).show(childFragmentManager, null)
        }
    }

    override fun onMovedToForeground() {
        if (this::viewModel.isInitialized && !wasInPictureInPictureMode) {
            viewModel.onResume()
        }
    }

    override fun onMovedToBackground() {
        if (this::viewModel.isInitialized && !wasInPictureInPictureMode) {
            viewModel.onPause()
        }
    }

    override fun onNetworkRestored() {
        if (this::viewModel.isInitialized) {
            viewModel.onResume()
        }
    }

    override fun getCurrentPosition(): Double {
        return runBlocking(Dispatchers.Main) { viewModel.player.currentPosition / 1000.0 }
    }
}
