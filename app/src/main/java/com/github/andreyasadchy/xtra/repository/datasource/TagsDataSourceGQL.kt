package com.github.andreyasadchy.xtra.repository.datasource

import androidx.paging.DataSource
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import kotlinx.coroutines.CoroutineScope

class TagsDataSourceGQL private constructor(
    private val clientId: String?,
    private val getGameTags: Boolean,
    private val gameId: String?,
    private val gameName: String?,
    private val query: String?,
    private val api: GraphQLRepository,
    coroutineScope: CoroutineScope) : BasePositionalDataSource<Tag>(coroutineScope) {

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Tag>) {
        loadInitial(params, callback) {
            if (gameId != null && gameName != null) {
                if (query.isNullOrBlank()) {
                    val get = api.loadGameStreamTags(clientId, gameName)
                    get.data.ifEmpty { listOf() }
                } else {
                    val search = api.loadSearchGameTags(clientId, gameId, query)
                    search.data.ifEmpty { listOf() }
                }
            } else {
                if (query.isNullOrBlank()) {
                    if (getGameTags) {
                        if (savedGameTags == null) {
                            val get = api.loadGameTags(clientId)
                            if (get.data.isNotEmpty()) {
                                savedGameTags = get.data
                                get.data
                            } else listOf()
                        } else savedGameTags ?: listOf()
                    } else {
                        if (savedAllTags == null) {
                            val get = api.loadStreamTags(clientId)
                            if (get.data.isNotEmpty()) {
                                savedAllTags = get.data
                                get.data
                            } else listOf()
                        } else savedAllTags ?: listOf()
                    }
                } else {
                    val search = api.loadSearchAllTags(clientId, query)
                    search.data.ifEmpty { listOf() }
                }
            }
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Tag>) {
        loadRange(params, callback) {
            listOf()
        }
    }

    class Factory(
        private val clientId: String?,
        private val getGameTags: Boolean,
        private val gameId: String?,
        private val gameName: String?,
        private val query: String?,
        private val api: GraphQLRepository,
        private val coroutineScope: CoroutineScope) : BaseDataSourceFactory<Int, Tag, TagsDataSourceGQL>() {

        override fun create(): DataSource<Int, Tag> =
                TagsDataSourceGQL(clientId, getGameTags, gameId, gameName, query, api, coroutineScope).also(sourceLiveData::postValue)
    }

    companion object {
        private var savedAllTags: List<Tag>? = null
        private var savedGameTags: List<Tag>? = null
    }
}
