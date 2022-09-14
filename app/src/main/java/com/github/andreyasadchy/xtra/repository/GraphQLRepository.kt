package com.github.andreyasadchy.xtra.repository

import android.util.Log
import com.github.andreyasadchy.xtra.api.GraphQLApi
import com.github.andreyasadchy.xtra.model.chat.EmoteCardResponse
import com.github.andreyasadchy.xtra.model.gql.channel.ChannelClipsDataResponse
import com.github.andreyasadchy.xtra.model.gql.channel.ChannelHostingDataResponse
import com.github.andreyasadchy.xtra.model.gql.channel.ChannelVideosDataResponse
import com.github.andreyasadchy.xtra.model.gql.channel.ChannelViewerListDataResponse
import com.github.andreyasadchy.xtra.model.gql.clip.ClipDataResponse
import com.github.andreyasadchy.xtra.model.gql.clip.ClipVideoResponse
import com.github.andreyasadchy.xtra.model.gql.emote.UserEmotesDataResponse
import com.github.andreyasadchy.xtra.model.gql.followed.*
import com.github.andreyasadchy.xtra.model.gql.game.GameClipsDataResponse
import com.github.andreyasadchy.xtra.model.gql.game.GameDataResponse
import com.github.andreyasadchy.xtra.model.gql.game.GameStreamsDataResponse
import com.github.andreyasadchy.xtra.model.gql.game.GameVideosDataResponse
import com.github.andreyasadchy.xtra.model.gql.points.ChannelPointsContextDataResponse
import com.github.andreyasadchy.xtra.model.gql.search.SearchChannelDataResponse
import com.github.andreyasadchy.xtra.model.gql.search.SearchGameDataResponse
import com.github.andreyasadchy.xtra.model.gql.search.SearchVideosDataResponse
import com.github.andreyasadchy.xtra.model.gql.stream.StreamDataResponse
import com.github.andreyasadchy.xtra.model.gql.stream.ViewersDataResponse
import com.github.andreyasadchy.xtra.model.gql.tag.*
import com.github.andreyasadchy.xtra.model.gql.vod.VodGamesDataResponse
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GraphQLRepository"

@Singleton
class GraphQLRepository @Inject constructor(private val graphQL: GraphQLApi) {

    suspend fun loadClipUrls(clientId: String?, slug: String?): Map<String, String> = withContext(Dispatchers.IO) {
        val array = JsonArray(1)
        val videoAccessTokenOperation = JsonObject().apply {
            addProperty("operationName", "VideoAccessToken_Clip")
            add("variables", JsonObject().apply {
                addProperty("slug", slug)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "36b89d2507fce29e5ca551df756d27c1cfe079e2609642b4390aa4c35796eb11")
                })
            })
        }
        array.add(videoAccessTokenOperation)
        val response = graphQL.getClipUrls(clientId, array)
        response.data.withIndex().associateBy({
            if (!it.value.quality.isNullOrBlank()) {
                if ((it.value.frameRate ?: 0) < 60) {
                    "${it.value.quality}p"
                } else {
                    "${it.value.quality}p${it.value.frameRate}"
                }
            } else {
                it.index.toString()
            }
        }, { it.value.url })
    }

    suspend fun loadClipData(clientId: String?, slug: String?): ClipDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChannelClipCore")
            add("variables", JsonObject().apply {
                addProperty("clipSlug", slug)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "16d402536bdd88b9db9a7cc87da5769607676abf22ad46b6cfab57a2b8b0b20e")
                })
            })
        }
        return graphQL.getClipData(clientId, json)
    }

    suspend fun loadClipVideo(clientId: String?, slug: String?): ClipVideoResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChatClip")
            add("variables", JsonObject().apply {
                addProperty("clipSlug", slug)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "9aa558e066a22227c5ef2c0a8fded3aaa57d35181ad15f63df25bff516253a90")
                })
            })
        }
        return graphQL.getClipVideo(clientId, json)
    }

    suspend fun loadTopGames(clientId: String?, tags: List<String>?, limit: Int?, cursor: String?): GameDataResponse {
        val array = JsonArray()
        if (tags != null) {
            for (i in tags) {
                array.add(i)
            }
        }
        val json = JsonObject().apply {
            addProperty("operationName", "BrowsePage_AllDirectories")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
                add("options", JsonObject().apply {
                    addProperty("sort", "VIEWER_COUNT")
                    add("tags", array)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "78957de9388098820e222c88ec14e85aaf6cf844adf44c8319c545c75fd63203")
                })
            })
        }
        return graphQL.getTopGames(clientId, json)
    }

    suspend fun loadTopStreams(clientId: String?, tags: List<String>?, limit: Int?, cursor: String?): StreamDataResponse {
        val array = JsonArray()
        if (tags != null) {
            for (i in tags) {
                array.add(i)
            }
        }
        val json = JsonObject().apply {
            addProperty("operationName", "BrowsePage_Popular")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
                addProperty("platformType", "all")
                addProperty("sortTypeIsRecency", false)
                add("options", JsonObject().apply {
                    addProperty("sort", "VIEWER_COUNT")
                    add("tags", array)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "4de7f2166105c1a034ba40251f55593b90500f69cf44c8735db4f62ad2760c39")
                })
            })
        }
        return graphQL.getTopStreams(clientId, json)
    }

    suspend fun loadGameStreams(clientId: String?, game: String?, sort: String?, tags: List<String>?, limit: Int?, cursor: String?): GameStreamsDataResponse {
        val array = JsonArray()
        if (tags != null) {
            for (i in tags) {
                array.add(i)
            }
        }
        val json = JsonObject().apply {
            addProperty("operationName", "DirectoryPage_Game")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
                addProperty("name", game)
                addProperty("sortTypeIsRecency", false)
                add("options", JsonObject().apply {
                    addProperty("sort", sort)
                    add("tags", array)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "d5c5df7ab9ae65c3ea0f225738c08a36a4a76e4c6c31db7f8c4b8dc064227f9e")
                })
            })
        }
        return graphQL.getGameStreams(clientId, json)
    }

    suspend fun loadGameVideos(clientId: String?, game: String?, type: String?, sort: String?, limit: Int?, cursor: String?): GameVideosDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "DirectoryVideos_Game")
            add("variables", JsonObject().apply {
                if (type != null) {
                    addProperty("broadcastTypes", type)
                }
                addProperty("followedCursor", cursor)
                addProperty("gameName", game)
                addProperty("videoLimit", limit)
                addProperty("videoSort", sort)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "c04a45b3adfcfacdff2bf4c4172ca4904870d62d6d19f3d490705c5d0a9e511e")
                })
            })
        }
        return graphQL.getGameVideos(clientId, json)
    }

    suspend fun loadGameClips(clientId: String?, game: String?, sort: String?, limit: Int?, cursor: String?): GameClipsDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ClipsCards__Game")
            add("variables", JsonObject().apply {
                add("criteria", JsonObject().apply {
                    addProperty("filter", sort)
                })
                addProperty("cursor", cursor)
                addProperty("gameName", game)
                addProperty("limit", limit)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "0d8d0eba9fc7ef77de54a7d933998e21ad7a1274c867ec565ac14ffdce77b1f9")
                })
            })
        }
        return graphQL.getGameClips(clientId, json)
    }

    suspend fun loadChannelVideos(clientId: String?, channel: String?, type: String?, sort: String?, limit: Int?, cursor: String?): ChannelVideosDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FilterableVideoTower_Videos")
            add("variables", JsonObject().apply {
                addProperty("broadcastType", type)
                addProperty("cursor", cursor)
                addProperty("channelOwnerLogin", channel)
                addProperty("limit", limit)
                addProperty("videoSort", sort)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "a937f1d22e269e39a03b509f65a7490f9fc247d7f83d6ac1421523e3b68042cb")
                })
            })
        }
        return graphQL.getChannelVideos(clientId, json)
    }

    suspend fun loadChannelClips(clientId: String?, channel: String?, sort: String?, limit: Int?, cursor: String?): ChannelClipsDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ClipsCards__User")
            add("variables", JsonObject().apply {
                add("criteria", JsonObject().apply {
                    addProperty("filter", sort)
                })
                addProperty("cursor", cursor)
                addProperty("login", channel)
                addProperty("limit", limit)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "b73ad2bfaecfd30a9e6c28fada15bd97032c83ec77a0440766a56fe0bd632777")
                })
            })
        }
        return graphQL.getChannelClips(clientId, json)
    }

    suspend fun loadChannelViewerList(clientId: String?, channelLogin: String?): ChannelViewerListDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChatViewers")
            add("variables", JsonObject().apply {
                addProperty("channelLogin", channelLogin)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "e0761ef5444ee3acccee5cfc5b834cbfd7dc220133aa5fbefe1b66120f506250")
                })
            })
        }
        return graphQL.getChannelViewerList(clientId, json)
    }

    suspend fun loadSearchChannels(clientId: String?, query: String?, cursor: String?): SearchChannelDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchResultsPage_SearchResults")
            add("variables", JsonObject().apply {
                add("options", JsonObject().apply {
                    add("targets", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("cursor", cursor)
                            addProperty("index", "CHANNEL")
                        })
                    })
                })
                addProperty("query", query)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "ee977ac21b324669b4c109be49ed3032227e8850bea18503d0ced68e8156c2a5")
                })
            })
        }
        return graphQL.getSearchChannels(clientId, json)
    }

    suspend fun loadSearchGames(clientId: String?, query: String?, cursor: String?): SearchGameDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchResultsPage_SearchResults")
            add("variables", JsonObject().apply {
                add("options", JsonObject().apply {
                    add("targets", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("cursor", cursor)
                            addProperty("index", "GAME")
                        })
                    })
                })
                addProperty("query", query)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "ee977ac21b324669b4c109be49ed3032227e8850bea18503d0ced68e8156c2a5")
                })
            })
        }
        return graphQL.getSearchGames(clientId, json)
    }

    suspend fun loadSearchVideos(clientId: String?, query: String?, cursor: String?): SearchVideosDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchResultsPage_SearchResults")
            add("variables", JsonObject().apply {
                add("options", JsonObject().apply {
                    add("targets", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("cursor", cursor)
                            addProperty("index", "VOD")
                        })
                    })
                })
                addProperty("query", query)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "ee977ac21b324669b4c109be49ed3032227e8850bea18503d0ced68e8156c2a5")
                })
            })
        }
        return graphQL.getSearchVideos(clientId, json)
    }

    suspend fun loadGameTags(clientId: String?): TagGameDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchCategoryTags")
            add("variables", JsonObject().apply {
                addProperty("limit", 100)
                addProperty("userQuery", "")
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "e8811c72159b644b87971a4f36872028355ce5349eb8b7fb5cc478cd79d4fd92")
                })
            })
        }
        return graphQL.getGameTags(clientId, json)
    }

    suspend fun loadGameStreamTags(clientId: String?, gameName: String? = null): TagGameStreamDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "TopTags")
            add("variables", JsonObject().apply {
                addProperty("categoryName", gameName)
                addProperty("limit", 10000)
                addProperty("showTopTagsByCategory", gameName != null)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "16c030e5b84bf696f9cf25bee7ddde328c93f2b481a0519a806c19d0e91ab9c1")
                })
            })
        }
        return graphQL.getGameStreamTags(clientId, json)
    }

    suspend fun loadStreamTags(clientId: String?): TagStreamDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "TopTags")
            add("variables", JsonObject().apply {
                addProperty("limit", 10000)
                addProperty("showTopTagsByCategory", false)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "16c030e5b84bf696f9cf25bee7ddde328c93f2b481a0519a806c19d0e91ab9c1")
                })
            })
        }
        return graphQL.getStreamTags(clientId, json)
    }

    suspend fun loadSearchGameTags(clientId: String?, gameId: String?, query: String?): TagSearchGameStreamDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchSingleCategoryTags")
            add("variables", JsonObject().apply {
                addProperty("categoryID", gameId)
                addProperty("limit", 100)
                addProperty("userQuery", query)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "0928a2ec27d51a3be8562d1b724a4b03164b94d26e415be1485a7c6230eb5cac")
                })
            })
        }
        return graphQL.getSearchGameTags(clientId, json)
    }

    suspend fun loadSearchAllTags(clientId: String?, query: String?): TagSearchDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "SearchLiveTags")
            add("variables", JsonObject().apply {
                addProperty("limit", 100)
                addProperty("userQuery", query)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "e184321982c7a58cb6c819e9b7acdac8892f50a9501cd3a36e8cdbd0e427aeac")
                })
            })
        }
        return graphQL.getSearchStreamTags(clientId, json)
    }

    suspend fun loadVodGames(clientId: String?, videoId: String?): VodGamesDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "VideoPlayer_ChapterSelectButtonVideo")
            add("variables", JsonObject().apply {
                addProperty("videoID", videoId)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "8d2793384aac3773beab5e59bd5d6f585aedb923d292800119e03d40cd0f9b41")
                })
            })
        }
        return graphQL.getVodGames(clientId, json)
    }

    suspend fun loadViewerCount(clientId: String?, channelLogin: String?): ViewersDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "UseViewCount")
            add("variables", JsonObject().apply {
                addProperty("channelLogin", channelLogin)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "00b11c9c428f79ae228f30080a06ffd8226a1f068d6f52fbc057cbde66e994c2")
                })
            })
        }
        return graphQL.getViewerCount(clientId, json)
    }

    suspend fun loadChannelHosting(clientId: String?, channelLogin: String?): ChannelHostingDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "UseHosting")
            add("variables", JsonObject().apply {
                addProperty("channelLogin", channelLogin)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "427f55a3daca510f726c02695a898ef3a0de4355b39af328848876052ea6b337")
                })
            })
        }
        return graphQL.getChannelHosting(clientId, json)
    }

    suspend fun loadEmoteCard(clientId: String?, emoteId: String?): EmoteCardResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "EmoteCard")
            add("variables", JsonObject().apply {
                addProperty("emoteID", emoteId)
                addProperty("octaneEnabled", true)
                addProperty("artistEnabled", true)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "556230dd63957761355ba54232c43f4781f31ed6686fc827053b9aa7b199848f")
                })
            })
        }
        return graphQL.getEmoteCard(clientId, json)
    }

    suspend fun loadFollowedStreams(clientId: String?, token: String?, limit: Int?, cursor: String?): FollowedStreamsDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowingLive_CurrentUser")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "40ac5a060fa06ba73e07bf8dd8c3cf6aca4494aeed2222c986ed47ffddf31f51")
                })
            })
        }
        return graphQL.getFollowedStreams(clientId, token, json)
    }

    suspend fun loadFollowedVideos(clientId: String?, token: String?, limit: Int?, cursor: String?): FollowedVideosDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowedVideos_CurrentUser")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "a8e02d4cc25511e9997842c80333e15ba0bb9e11b4199e31c5207317faff9618")
                })
            })
        }
        return graphQL.getFollowedVideos(clientId, token, json)
    }

    suspend fun loadFollowedChannels(clientId: String?, token: String?, limit: Int?, cursor: String?): FollowedChannelsDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChannelFollows")
            add("variables", JsonObject().apply {
                addProperty("cursor", cursor)
                addProperty("limit", limit)
                addProperty("order", "DESC")
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "4b9cb31b54b9213e5760f2f6e9e935ad09924cac2f78aac51f8a64d85f028ed0")
                })
            })
        }
        return graphQL.getFollowedChannels(clientId, token, json)
    }

    suspend fun loadFollowedGames(clientId: String?, token: String?, limit: Int?): FollowedGamesDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowingGames_CurrentUser")
            add("variables", JsonObject().apply {
                addProperty("limit", limit)
                addProperty("type", "ALL")
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "8446d4d234005813dc1f024f487ce95434c3e4202f451dd42777935b5ed035ce")
                })
            })
        }
        return graphQL.getFollowedGames(clientId, token, json)
    }

    suspend fun loadFollowUser(clientId: String?, token: String?, integrityToken: String?, userId: String?): FollowUserDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowButton_FollowUser")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("disableNotifications", false)
                    addProperty("targetID", userId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "800e7346bdf7e5278a3c1d3f21b2b56e2639928f86815677a7126b093b2fdd08")
                })
            })
        }
        val randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        return graphQL.getFollowUser(clientId, token, integrityToken, randomId, json)
    }

    suspend fun loadUnfollowUser(clientId: String?, token: String?, integrityToken: String?, userId: String?): JsonElement {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowButton_UnfollowUser")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("targetID", userId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "f7dae976ebf41c755ae2d758546bfd176b4eeb856656098bb40e0a672ca0d880")
                })
            })
        }
        val randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        return graphQL.getUnfollowUser(clientId, token, integrityToken, randomId, json)
    }

    suspend fun loadFollowGame(clientId: String?, token: String?, integrityToken: String?, gameId: String?): JsonElement {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowGameButton_FollowGame")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("gameID", gameId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "b846b65ba4bc9a3561dbe2d069d95deed9b9e031bcfda2482d1bedd84a1c2eb3")
                })
            })
        }
        val randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        return graphQL.getFollowGame(clientId, token, integrityToken, randomId, json)
    }

    suspend fun loadUnfollowGame(clientId: String?, token: String?, integrityToken: String?, gameId: String?): JsonElement {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowGameButton_UnfollowGame")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("gameID", gameId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "811e02e396ebba0664f21ff002f2eff3c6f57e8af9aedb4f4dfa77cefd0db43d")
                })
            })
        }
        val randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        return graphQL.getUnfollowGame(clientId, token, integrityToken, randomId, json)
    }

    suspend fun loadFollowingUser(clientId: String?, token: String?, userLogin: String?): FollowingUserDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChannelSupportButtons")
            add("variables", JsonObject().apply {
                addProperty("channelLogin", userLogin)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "834a75e1c06cffada00f0900664a5033e392f6fb655fae8d2e25b21b340545a9")
                })
            })
        }
        return graphQL.getFollowingUser(clientId, token, json)
    }

    suspend fun loadFollowingGame(clientId: String?, token: String?, gameName: String?): FollowingGameDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "FollowGameButton_Game")
            add("variables", JsonObject().apply {
                addProperty("name", gameName)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "cfeda60899b6b867b2d7f30c8556778c4a9cc8268bd1aadd9f88134a0f642a02")
                })
            })
        }
        return graphQL.getFollowingGame(clientId, token, json)
    }

    suspend fun loadChannelPointsContext(clientId: String?, token: String?, channelLogin: String?): ChannelPointsContextDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "ChannelPointsContext")
            add("variables", JsonObject().apply {
                addProperty("channelLogin", channelLogin)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "1530a003a7d374b0380b79db0be0534f30ff46e61cffa2bc0e2468a909fbc024")
                })
            })
        }
        return graphQL.getChannelPointsContext(clientId, token, json)
    }

    suspend fun loadClaimPoints(clientId: String?, token: String?, channelId: String?, claimId: String?) {
        val json = JsonObject().apply {
            addProperty("operationName", "ClaimCommunityPoints")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("channelID", channelId)
                    addProperty("claimID", claimId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "46aaeebe02c99afdf4fc97c7c0cba964124bf6b0af229395f1f6d1feed05b3d0")
                })
            })
        }
        return graphQL.getClaimPoints(clientId, token, json)
    }

    suspend fun loadJoinRaid(clientId: String?, token: String?, raidId: String?) {
        val json = JsonObject().apply {
            addProperty("operationName", "JoinRaid")
            add("variables", JsonObject().apply {
                add("input", JsonObject().apply {
                    addProperty("raidID", raidId)
                })
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "c6a332a86d1087fbbb1a8623aa01bd1313d2386e7c63be60fdb2d1901f01a4ae")
                })
            })
        }
        return graphQL.getJoinRaid(clientId, token, json)
    }

    suspend fun loadUserEmotes(clientId: String?, token: String?, channelId: String?): UserEmotesDataResponse {
        val json = JsonObject().apply {
            addProperty("operationName", "AvailableEmotesForChannel")
            add("variables", JsonObject().apply {
                addProperty("channelID", channelId)
                addProperty("withOwner", true)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "b9ce64d02e26c6fe9adbfb3991284224498b295542f9c5a51eacd3610e659cfb")
                })
            })
        }
        return graphQL.getUserEmotes(clientId, token, json)
    }

    suspend fun loadChannelPanel(channelId: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading panel for channel: $channelId")
        val array = JsonArray(1)
        val panelOperation = JsonObject().apply {
            addProperty("operationName", "ChannelPanels")
            add("variables", JsonObject().apply {
                addProperty("id", channelId)
            })
            add("extensions", JsonObject().apply {
                add("persistedQuery", JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("sha256Hash", "236b0ec07489e5172ee1327d114172f27aceca206a1a8053106d60926a7f622e")
                })
            })
        }
        array.add(panelOperation)
        graphQL.getChannelPanel(array).body()?.string()
    }
}