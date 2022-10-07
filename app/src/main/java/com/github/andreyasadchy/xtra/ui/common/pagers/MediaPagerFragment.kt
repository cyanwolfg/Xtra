package com.github.andreyasadchy.xtra.ui.common.pagers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.common.BaseNetworkFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.util.reduceDragSensitivity
import kotlinx.android.synthetic.main.fragment_media_pager.*

abstract class MediaPagerFragment : BaseNetworkFragment(), ItemAwarePagerFragment, Scrollable {

    protected lateinit var adapter: FragmentStateAdapter
    private var firstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstLaunch = savedInstanceState == null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_pager, container, false)
    }

    protected fun setAdapter(adapter: FragmentStateAdapter, defaultItem: Int? = null) {
        this.adapter = adapter
        viewPager.adapter = adapter
        if (firstLaunch && defaultItem != null) {
            viewPager.setCurrentItem(defaultItem, false)
            firstLaunch = false
        }
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.reduceDragSensitivity()
    }

    override fun scrollToTop() {
        (currentFragment as? Scrollable)?.scrollToTop()
    }
}
