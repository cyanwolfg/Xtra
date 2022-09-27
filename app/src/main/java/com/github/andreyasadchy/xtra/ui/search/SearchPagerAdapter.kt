package com.github.andreyasadchy.xtra.ui.search

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.ui.search.channels.ChannelSearchFragment
import com.github.andreyasadchy.xtra.ui.search.games.GameSearchFragment
import com.github.andreyasadchy.xtra.ui.search.streams.StreamSearchFragment
import com.github.andreyasadchy.xtra.ui.search.videos.VideoSearchFragment

class SearchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VideoSearchFragment()
            1 -> StreamSearchFragment()
            2 -> ChannelSearchFragment()
            else -> GameSearchFragment()
        }
    }

    override fun getItemCount(): Int = 4
}
