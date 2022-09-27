package com.github.andreyasadchy.xtra.ui.follow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerToolbarFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.fragment_media_pager.*
import kotlinx.android.synthetic.main.fragment_media_pager.view.*

class FollowPagerFragment : MediaPagerToolbarFragment() {

    companion object {
        private const val DEFAULT_ITEM = "default_item"
        private const val LOGGED_IN = "logged_in"

        fun newInstance(defaultItem: Int?, loggedIn: Boolean) = FollowPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(DEFAULT_ITEM, defaultItem ?: 0)
                putBoolean(LOGGED_IN, loggedIn)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val defaultItem = requireArguments().getInt(DEFAULT_ITEM)
        val loggedIn = requireArguments().getBoolean(LOGGED_IN)
        setAdapter(adapter = FollowPagerAdapter(this, loggedIn),
            defaultItem = if (loggedIn) {
                when (defaultItem) {
                    1 -> 2
                    2 -> 3
                    3 -> 0
                    else -> 1
                }
            } else {
                when (defaultItem) {
                    2 -> 2
                    3 -> 0
                    else -> 1
                }
            })
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (loggedIn) {
                when (position) {
                    0 -> getString(R.string.games)
                    1 -> getString(R.string.live)
                    2 -> getString(R.string.videos)
                    else -> getString(R.string.channels)
                }
            } else {
                when (position) {
                    0 -> getString(R.string.games)
                    1 -> getString(R.string.live)
                    else -> getString(R.string.channels)
                }
            }
        }.attach()
    }

    override val currentFragment: Fragment?
        get() = childFragmentManager.findFragmentByTag("f${pagerLayout.viewPager.currentItem}")

    override fun initialize() {
    }

    override fun onNetworkRestored() {
    }
}