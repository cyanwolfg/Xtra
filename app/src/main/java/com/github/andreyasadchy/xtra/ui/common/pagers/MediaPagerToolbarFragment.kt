package com.github.andreyasadchy.xtra.ui.common.pagers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import kotlinx.android.synthetic.main.fragment_media_pager_toolbar.*

abstract class MediaPagerToolbarFragment : MediaPagerFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_pager_toolbar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val user = User.get(activity)
        search.setOnClickListener { activity.openSearch() }
        menu.setOnClickListener { it ->
            PopupMenu(activity, it).apply {
                inflate(R.menu.top_menu)
                menu.findItem(R.id.login).title = if (user !is NotLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.settings -> { activity.startActivityFromFragment(this@MediaPagerToolbarFragment, Intent(activity, SettingsActivity::class.java), 3) }
                        R.id.login -> {
                            if (user is NotLoggedIn) {
                                activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                            } else {
                                AlertDialog.Builder(activity).apply {
                                    setTitle(getString(R.string.logout_title))
                                    user.login?.nullIfEmpty()?.let { user -> setMessage(getString(R.string.logout_msg, user)) }
                                    setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                    setPositiveButton(getString(R.string.yes)) { _, _ -> activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2) }
                                }.show()
                            }
                        }
                    }
                    true
                }
                show()
            }
        }
    }

    override fun scrollToTop() {
        appBar?.setExpanded(true, true)
        (currentFragment as? Scrollable)?.scrollToTop()
    }
}
