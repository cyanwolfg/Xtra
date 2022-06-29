package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SearchVideosDataDeserializer : JsonDeserializer<SearchVideosDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SearchVideosDataResponse {
        val data = mutableListOf<Video>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("searchFor")?.getAsJsonObject("videos")
        val cursor = dataJson?.getAsJsonPrimitive("cursor")?.asString
        dataJson?.getAsJsonArray("edges")?.forEach {
            it?.asJsonObject?.getAsJsonObject("item")?.let { obj ->
                val tags = mutableListOf<Tag>()
                obj.getAsJsonArray("contentTags")?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.asJsonObject?.getAsJsonPrimitive("id")?.asString,
                        name = tag.asJsonObject?.getAsJsonPrimitive("localizedName")?.asString
                    ))
                }
                data.add(Video(
                    id = obj.getAsJsonPrimitive("id")?.asString ?: "",
                    user_id = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("id")?.asString,
                    user_login = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("login")?.asString,
                    user_name = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("displayName")?.asString,
                    title = obj.getAsJsonPrimitive("title")?.asString,
                    createdAt = obj.getAsJsonPrimitive("createdAt")?.asString,
                    thumbnail_url = obj.getAsJsonPrimitive("previewThumbnailURL")?.asString,
                    view_count = obj.getAsJsonPrimitive("viewCount")?.asInt,
                    duration = obj.getAsJsonPrimitive("lengthSeconds")?.asString,
                    gameId = obj.getAsJsonObject("game").getAsJsonPrimitive("id")?.asString,
                    gameName = obj.getAsJsonObject("game").getAsJsonPrimitive("displayName")?.asString,
                    tags = tags
                ))
            }
        }
        return SearchVideosDataResponse(data, cursor)
    }
}
