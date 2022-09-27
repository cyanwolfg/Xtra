package com.github.andreyasadchy.xtra.ui.search.tags

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class BaseTagSearchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return TagSearchFragment()
    }

    override fun getItemCount(): Int = 1
}
