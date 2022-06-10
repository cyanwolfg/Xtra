package com.github.andreyasadchy.xtra.ui.search.tags

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.andreyasadchy.xtra.ui.common.pagers.ItemAwareFragmentPagerAdapter

class BaseTagSearchPagerAdapter(fm: FragmentManager) : ItemAwareFragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return TagSearchFragment()
    }

    override fun getCount(): Int = 1
}
