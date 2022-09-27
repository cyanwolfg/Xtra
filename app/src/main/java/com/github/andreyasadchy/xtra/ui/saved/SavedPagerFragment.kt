package com.github.andreyasadchy.xtra.ui.saved

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerToolbarFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.fragment_media_pager.*
import kotlinx.android.synthetic.main.fragment_media_pager.view.*

class SavedPagerFragment : MediaPagerToolbarFragment() {

    companion object {
        private const val DEFAULT_ITEM = "default_item"

        fun newInstance(defaultItem: Int?) = SavedPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(DEFAULT_ITEM, defaultItem ?: 0)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val defaultItem = requireArguments().getInt(DEFAULT_ITEM)
        setAdapter(adapter = SavedPagerAdapter(this), defaultItem = defaultItem)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.bookmarks)
                else -> getString(R.string.downloads)
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