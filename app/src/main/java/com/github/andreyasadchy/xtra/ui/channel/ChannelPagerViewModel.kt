package com.github.andreyasadchy.xtra.ui.channel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import com.github.andreyasadchy.xtra.util.DownloadUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class ChannelPagerViewModel @Inject constructor(
    private val repository: TwitchService,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository) : ViewModel(), FollowViewModel {

    private val _stream = MutableLiveData<Stream?>()
    val stream: MutableLiveData<Stream?>
        get() = _stream
    private val _user = MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>()
    val user: MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>
        get() = _user

    private val _userId = MutableLiveData<String?>()
    private val _userLogin = MutableLiveData<String?>()
    private val _userName = MutableLiveData<String?>()
    private val _profileImageURL = MutableLiveData<String?>()
    override val userId: String?
        get() { return _userId.value }
    override val userLogin: String?
        get() { return _userLogin.value }
    override val userName: String?
        get() { return _userName.value }
    override val channelLogo: String?
        get() { return _profileImageURL.value }
    override lateinit var follow: FollowLiveData

    override fun setUser(user: User, helixClientId: String?, gqlClientId: String?, setting: Int) {
        if (!this::follow.isInitialized) {
            follow = FollowLiveData(localFollowsChannel = localFollowsChannel, userId = userId, userLogin = userLogin, userName = userName, channelLogo = channelLogo, repository = repository, helixClientId = helixClientId, user = user, gqlClientId = gqlClientId, setting = setting, viewModelScope = viewModelScope)
        }
    }

    fun init(channelId: String?, channelLogin: String?, channelName: String?, profileImageURL: String?) {
        _userId.value = channelId
        _userLogin.value = channelLogin
        _userName.value = channelName
        _profileImageURL.value = profileImageURL
    }

    fun loadStream(helixClientId: String?, helixToken: String?, gqlClientId: String?) {
        viewModelScope.launch {
            try {
                repository.loadUserChannelPage(userId, userLogin, helixClientId, helixToken, gqlClientId)?.let { _stream.postValue(it) }
            } catch (e: Exception) {}
        }
    }

    fun loadUser(helixClientId: String?, helixToken: String?) {
        if (!helixToken.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    repository.loadUser(userId, userLogin, helixClientId, helixToken)?.let { _user.postValue(it) }
                } catch (e: Exception) {}
            }
        }
    }

    fun retry(helixClientId: String?, helixToken: String?, gqlClientId: String?) {
        if (_stream.value == null) {
            loadStream(helixClientId, helixToken, gqlClientId)
        } else {
            if (_stream.value?.channelUser == null && _user.value == null) {
                loadUser(helixClientId, helixToken)
            }
        }
    }

    fun updateLocalUser(context: Context, user: com.github.andreyasadchy.xtra.model.helix.user.User) {
        GlobalScope.launch {
            try {
                if (user.id != null) {
                    try {
                        Glide.with(context)
                            .asBitmap()
                            .load(user.channelLogo)
                            .into(object: CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    DownloadUtils.savePng(context, "profile_pics", user.id, resource)
                                }
                            })
                    } catch (e: Exception) {

                    }
                    val downloadedLogo = File(context.filesDir.toString() + File.separator + "profile_pics" + File.separator + "${user.id}.png").absolutePath
                    localFollowsChannel.getFollowById(user.id)?.let { localFollowsChannel.updateFollow(it.apply {
                        user_login = user.login
                        user_name = user.display_name
                        channelLogo = downloadedLogo }) }
                    for (i in offlineRepository.getVideosByUserId(user.id.toInt())) {
                        offlineRepository.updateVideo(i.apply {
                            channelLogin = user.login
                            channelName = user.display_name
                            channelLogo = downloadedLogo })
                    }
                    for (i in bookmarksRepository.getBookmarksByUserId(user.id)) {
                        bookmarksRepository.updateBookmark(i.apply {
                            userLogin = user.login
                            userName = user.display_name
                            userLogo = downloadedLogo })
                    }
                }
            } catch (e: Exception) {

            }
        }
    }
}
