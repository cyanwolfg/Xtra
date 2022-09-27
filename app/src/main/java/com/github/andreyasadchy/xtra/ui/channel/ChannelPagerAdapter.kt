package com.github.andreyasadchy.xtra.ui.channel

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.ui.chat.ChatFragment
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.videos.channel.ChannelVideosFragment
import com.github.andreyasadchy.xtra.util.C

class ChannelPagerAdapter(
        fragment: Fragment,
        private val args: Bundle) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChannelVideosFragment().apply { arguments = args }
            1 -> ClipsFragment().apply { arguments = args }
            else -> ChatFragment.newInstance(args.getString(C.CHANNEL_ID), args.getString(C.CHANNEL_LOGIN), args.getString(C.CHANNEL_DISPLAYNAME), args.getString(C.STREAM_ID))
        }
    }

    override fun getItemCount(): Int = 3
}
