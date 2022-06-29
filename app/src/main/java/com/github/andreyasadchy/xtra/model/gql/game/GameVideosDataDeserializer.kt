package com.github.andreyasadchy.xtra.model.gql.game

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class GameVideosDataDeserializer : JsonDeserializer<GameVideosDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GameVideosDataResponse {
        val data = mutableListOf<Video>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("game")?.getAsJsonObject("videos")?.getAsJsonArray("edges")
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.asString
        dataJson?.forEach {
            it?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
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
                    createdAt = obj.getAsJsonPrimitive("publishedAt")?.asString,
                    thumbnail_url = obj.getAsJsonPrimitive("previewThumbnailURL")?.asString,
                    view_count = obj.getAsJsonPrimitive("viewCount")?.asInt,
                    duration = obj.getAsJsonPrimitive("lengthSeconds")?.asString,
                    profileImageURL = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("profileImageURL")?.asString,
                    tags = tags
                ))
            }
        }
        return GameVideosDataResponse(data, cursor)
    }
}
