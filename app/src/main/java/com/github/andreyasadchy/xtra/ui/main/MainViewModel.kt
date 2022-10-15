package com.github.andreyasadchy.xtra.ui.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.LoggedIn
import com.github.andreyasadchy.xtra.model.NotValidated
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.AuthRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.util.Event
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import com.github.andreyasadchy.xtra.util.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: ApiRepository,
    private val authRepository: AuthRepository,
    private val offlineRepository: OfflineRepository) : ViewModel() {

    private val _isNetworkAvailable = MutableLiveData<Event<Boolean>>()
    val isNetworkAvailable: LiveData<Event<Boolean>>
        get() = _isNetworkAvailable

    var isPlayerMaximized = false
        private set

    var isPlayerOpened = false
        private set

    private val _video = MutableLiveData<Video?>()
    val video: MutableLiveData<Video?>
        get() = _video
    private val _clip = MutableLiveData<Clip?>()
    val clip: MutableLiveData<Clip?>
        get() = _clip
    private val _user = MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>()
    val user: MutableLiveData<com.github.andreyasadchy.xtra.model.helix.user.User?>
        get() = _user

    init {
        offlineRepository.resumeDownloads(application)
    }

    fun onMaximize() {
        isPlayerMaximized = true
    }

    fun onMinimize() {
        isPlayerMaximized = false
    }

    fun onPlayerStarted() {
        isPlayerOpened = true
        isPlayerMaximized = true
    }

    fun onPlayerClosed() {
        isPlayerOpened = false
        isPlayerMaximized = false
    }

    fun setNetworkAvailable(available: Boolean) {
        if (_isNetworkAvailable.value?.peekContent() != available) {
            _isNetworkAvailable.value = Event(available)
        }
    }

    fun loadVideo(videoId: String, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null) {
        _video.value = null
        viewModelScope.launch {
            try {
                repository.loadVideo(videoId, helixClientId, helixToken, gqlClientId)?.let { _video.postValue(it) }
            } catch (e: Exception) {}
        }
    }

    fun loadClip(clipId: String, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null) {
        _clip.value = null
        viewModelScope.launch {
            try {
                repository.loadClip(clipId, helixClientId, helixToken, gqlClientId)?.let { _clip.postValue(it) }
            } catch (e: Exception) {}
        }
    }

    fun loadUser(login: String? = null, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null) {
        _user.value = null
        viewModelScope.launch {
            try {
                repository.loadCheckUser(channelLogin = login, helixClientId = helixClientId, helixToken = helixToken, gqlClientId = gqlClientId)?.let { _user.postValue(it) }
            } catch (e: Exception) {}
        }
    }

    fun validate(helixClientId: String?, gqlClientId: String?, gqlClientId2: String?, activity: Activity) {
        val user = User.get(activity)
        if (user is NotValidated) {
            viewModelScope.launch {
                try {
                    if (!user.helixToken.isNullOrBlank()) {
                        val response = authRepository.validate(TwitchApiHelper.addTokenPrefixHelix(user.helixToken))
                        if (!response?.clientId.isNullOrBlank() && response?.clientId == helixClientId) {
                            if ((!response?.userId.isNullOrBlank() && response?.userId != user.id) || (!response?.login.isNullOrBlank() && response?.login != user.login)) {
                                User.set(activity, LoggedIn(response?.userId?.nullIfEmpty() ?: user.id, response?.login?.nullIfEmpty() ?: user.login, user.helixToken, user.gqlToken, user.gqlToken2))
                            }
                        } else {
                            throw IllegalStateException("401")
                        }
                    }
                    if (!user.gqlToken.isNullOrBlank()) {
                        val response = authRepository.validate(TwitchApiHelper.addTokenPrefixGQL(user.gqlToken))
                        if (!response?.clientId.isNullOrBlank() && response?.clientId == gqlClientId) {
                            if ((!response?.userId.isNullOrBlank() && response?.userId != user.id) || (!response?.login.isNullOrBlank() && response?.login != user.login)) {
                                User.set(activity, LoggedIn(response?.userId?.nullIfEmpty() ?: user.id, response?.login?.nullIfEmpty() ?: user.login, user.helixToken, user.gqlToken, user.gqlToken2))
                            }
                        } else {
                            throw IllegalStateException("401")
                        }
                    }
                    if (!user.gqlToken2.isNullOrBlank()) {
                        val response = authRepository.validate(TwitchApiHelper.addTokenPrefixGQL(user.gqlToken2))
                        if (!response?.clientId.isNullOrBlank() && response?.clientId == gqlClientId2) {
                            if ((!response?.userId.isNullOrBlank() && response?.userId != user.id) || (!response?.login.isNullOrBlank() && response?.login != user.login)) {
                                User.set(activity, LoggedIn(response?.userId?.nullIfEmpty() ?: user.id, response?.login?.nullIfEmpty() ?: user.login, user.helixToken, user.gqlToken, user.gqlToken2))
                            }
                        } else {
                            throw IllegalStateException("401")
                        }
                    }
                    if (!user.helixToken.isNullOrBlank() || !user.gqlToken.isNullOrBlank() || !user.gqlToken2.isNullOrBlank()) {
                        User.validated()
                    }
                } catch (e: Exception) {
                    if ((e is IllegalStateException && e.message == "401") || (e is HttpException && e.code() == 401)) {
                        User.set(activity, null)
                        activity.toast(R.string.token_expired)
                        activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2)
                    }
                }
            }
        }
        TwitchApiHelper.checkedValidation = true
    }
}