package com.github.andreyasadchy.xtra.ui.saved

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.andreyasadchy.xtra.ui.saved.bookmarks.BookmarksFragment
import com.github.andreyasadchy.xtra.ui.saved.downloads.DownloadsFragment

class SavedPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BookmarksFragment()
            else -> DownloadsFragment()
        }
    }

    override fun getItemCount(): Int = 2
}
