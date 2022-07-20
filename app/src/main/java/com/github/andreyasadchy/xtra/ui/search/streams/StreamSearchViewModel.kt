package com.github.andreyasadchy.xtra.ui.search.streams

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import javax.inject.Inject

class StreamSearchViewModel @Inject constructor(
        private val repository: TwitchService) : PagedListViewModel<Stream>() {

    private val query = MutableLiveData<String>()
    private var helixClientId = MutableLiveData<String>()
    private var helixToken = MutableLiveData<String>()
    private var gqlClientId = MutableLiveData<String>()
    private var apiPref = MutableLiveData<ArrayList<Pair<Long?, String?>?>>()
    private var thumbnailsEnabled = MutableLiveData<Boolean>()
    override val result: LiveData<Listing<Stream>> = Transformations.map(query) {
        repository.loadSearchStreams(it, helixClientId.value?.nullIfEmpty(), helixToken.value?.nullIfEmpty(), gqlClientId.value?.nullIfEmpty(), apiPref.value, thumbnailsEnabled.value, viewModelScope)
    }

    fun setQuery(query: String, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>, thumbnailsEnabled: Boolean = true) {
        if (this.helixClientId.value != helixClientId) {
            this.helixClientId.value = helixClientId
        }
        if (this.helixToken.value != helixToken) {
            this.helixToken.value = helixToken
        }
        if (this.gqlClientId.value != gqlClientId) {
            this.gqlClientId.value = gqlClientId
        }
        if (this.apiPref.value != apiPref) {
            this.apiPref.value = apiPref
        }
        if (this.thumbnailsEnabled.value != thumbnailsEnabled) {
            this.thumbnailsEnabled.value = thumbnailsEnabled
        }
        if (this.query.value != query) {
            this.query.value = query
        }
    }
}