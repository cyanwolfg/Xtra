package com.github.andreyasadchy.xtra.ui.follow.games

import android.app.Application
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import javax.inject.Inject

class FollowedGamesViewModel @Inject constructor(
        context: Application,
        private val repository: ApiRepository) : PagedListViewModel<Game>() {

    private val filter = MutableLiveData<Filter>()
    override val result: LiveData<Listing<Game>> = Transformations.map(filter) {
        repository.loadFollowedGames(it.user.id, it.gqlClientId, it.user.gqlToken, it.apiPref, viewModelScope)
    }

    fun setUser(user: User, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>) {
        if (filter.value == null) {
            filter.value = Filter(user, gqlClientId, apiPref)
        }
    }

    private data class Filter(
        val user: User,
        val gqlClientId: String?,
        val apiPref: ArrayList<Pair<Long?, String?>?>)
}
