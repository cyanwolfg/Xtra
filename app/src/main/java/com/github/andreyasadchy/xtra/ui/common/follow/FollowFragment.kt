package com.github.andreyasadchy.xtra.ui.common.follow

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.util.FragmentUtils
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.shortToast
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface FollowFragment {
    fun initializeFollow(fragment: Fragment, viewModel: FollowViewModel, followButton: ImageButton, setting: Int, user: User, helixClientId: String? = null, gqlClientId: String? = null) {
        val context = fragment.requireContext()
        with(viewModel) {
            setUser(user, helixClientId, gqlClientId, setting)
            followButton.visible()
            var initialized = false
            var errorMessage: String? = null
            follow.observe(fragment.viewLifecycleOwner) { following ->
                if (initialized) {
                    errorMessage.let {
                        if (it.isNullOrBlank()) {
                            context.shortToast(context.getString(if (following) R.string.now_following else R.string.unfollowed, userName))
                        } else {
                            context.shortToast(it)
                        }
                    }
                } else {
                    initialized = true
                }
                if ((!user.id.isNullOrBlank() && user.id == userId || !user.login.isNullOrBlank() && user.login == userLogin) && setting == 0 && !game) {
                    followButton.gone()
                } else {
                    if (errorMessage.isNullOrBlank()) {
                        followButton.setOnClickListener {
                            if (!following) {
                                GlobalScope.launch {
                                    errorMessage = if (game) {
                                        follow.saveFollowGame(context)
                                    } else {
                                        follow.saveFollowChannel(context)
                                    }
                                    follow.postValue(true)
                                }
                            } else {
                                FragmentUtils.showUnfollowDialog(context, userName) {
                                    GlobalScope.launch {
                                        errorMessage = if (game) {
                                            follow.deleteFollowGame(context)
                                        } else {
                                            follow.deleteFollowChannel(context)
                                        }
                                        follow.postValue(false)
                                    }
                                }
                            }
                        }
                        followButton.setImageResource(if (following) R.drawable.baseline_favorite_black_24 else R.drawable.baseline_favorite_border_black_24)
                    }
                }
            }
        }
    }
}