package com.github.andreyasadchy.xtra.ui.common.follow

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.util.FragmentUtils
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.shortToast
import com.github.andreyasadchy.xtra.util.visible

interface FollowFragment {
    fun initializeFollow(fragment: Fragment, viewModel: FollowViewModel, followButton: ImageButton, setting: Int, user: User, helixClientId: String? = null, gqlClientId: String? = null) {
        val context = fragment.requireContext()
        with(viewModel) {
            setUser(user, helixClientId, gqlClientId, setting)
            followButton.visible()
            var initialized = false
            follow.observe(fragment.viewLifecycleOwner) { following ->
                if (initialized) {
                    context.shortToast(context.getString(if (following) R.string.now_following else R.string.unfollowed, userName))
                } else {
                    initialized = true
                }
                if ((!user.id.isNullOrBlank() && user.id == userId || !user.login.isNullOrBlank() && user.login == userLogin) && setting == 0 && !game) {
                    followButton.gone()
                } else {
                    followButton.setOnClickListener {
                        if (!following) {
                            if (game) {
                                follow.saveFollowGame(context)
                            } else {
                                follow.saveFollowChannel(context)
                            }
                            follow.value = true
                        } else {
                            FragmentUtils.showUnfollowDialog(context, userName) {
                                if (game) {
                                    follow.deleteFollowGame(context)
                                } else {
                                    follow.deleteFollowChannel(context)
                                }
                                follow.value = false
                            }
                        }
                    }
                    followButton.setImageResource(if (following) R.drawable.baseline_favorite_black_24 else R.drawable.baseline_favorite_border_black_24)
                }
            }
        }
    }
}