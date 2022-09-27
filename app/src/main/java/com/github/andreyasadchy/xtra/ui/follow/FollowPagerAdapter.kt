package com.github.andreyasadchy.xtra.ui.follow

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.ui.follow.channels.FollowedChannelsFragment
import com.github.andreyasadchy.xtra.ui.follow.games.FollowedGamesFragment
import com.github.andreyasadchy.xtra.ui.streams.followed.FollowedStreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.followed.FollowedVideosFragment

class FollowPagerAdapter(
        fragment: Fragment,
        private val loggedIn: Boolean) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return if (loggedIn) {
            when (position) {
                0 -> FollowedGamesFragment()
                1 -> FollowedStreamsFragment()
                2 -> FollowedVideosFragment()
                else -> FollowedChannelsFragment()
            }
        } else {
            when (position) {
                0 -> FollowedGamesFragment()
                1 -> FollowedStreamsFragment()
                else -> FollowedChannelsFragment()
            }
        }
    }

    override fun getItemCount(): Int = if (loggedIn) 4 else 3
}
