package com.github.andreyasadchy.xtra.ui.search.videos

import android.app.Application
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosViewModel
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VideoSearchViewModel @Inject constructor(
    context: Application,
    private val repository: ApiRepository,
    playerRepository: PlayerRepository,
    bookmarksRepository: BookmarksRepository) : BaseVideosViewModel(playerRepository, bookmarksRepository, repository) {

    private val query = MutableLiveData<String>()
    private var gqlClientId = MutableLiveData<String>()
    private var apiPref = MutableLiveData<ArrayList<Pair<Long?, String?>?>>()
    override val result: LiveData<Listing<Video>> = Transformations.map(query) {
        repository.loadSearchVideos(it, gqlClientId.value?.nullIfEmpty(), apiPref.value, viewModelScope)
    }

    fun setQuery(query: String, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>) {
        if (this.gqlClientId.value != gqlClientId) {
            this.gqlClientId.value = gqlClientId
        }
        if (this.apiPref.value != apiPref) {
            this.apiPref.value = apiPref
        }
        if (this.query.value != query) {
            this.query.value = query
        }
    }
}