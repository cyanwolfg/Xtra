package com.github.andreyasadchy.xtra.model.gql.channel

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ChannelVideosDataDeserializer : JsonDeserializer<ChannelVideosDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChannelVideosDataResponse {
        val data = mutableListOf<Video>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("user")?.getAsJsonObject("videos")?.getAsJsonArray("edges")
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.takeIf { !it.isJsonNull }?.asString
        val hasNextPage = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("user")?.getAsJsonObject("videos")?.get("pageInfo")?.takeIf { !it.isJsonNull }?.asJsonObject?.get("hasNextPage")?.takeIf { !it.isJsonNull }?.asBoolean
        dataJson?.forEach { item ->
            item?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
                val tags = mutableListOf<Tag>()
                obj.get("contentTags")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { tagElement ->
                    tagElement?.takeIf { it.isJsonObject }?.asJsonObject.let { tag ->
                        tags.add(Tag(
                            id = tag?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                            name = tag?.get("localizedName")?.takeIf { !it.isJsonNull }?.asString,
                        ))
                    }
                }
                data.add(Video(
                    id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    user_id = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    user_login = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("login")?.takeIf { !it.isJsonNull }?.asString,
                    user_name = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString,
                    createdAt = obj.get("publishedAt")?.takeIf { !it.isJsonNull }?.asString,
                    thumbnail_url = obj.get("previewThumbnailURL")?.takeIf { !it.isJsonNull }?.asString,
                    view_count = obj.get("viewCount")?.takeIf { !it.isJsonNull }?.asInt,
                    duration = obj.get("lengthSeconds")?.takeIf { !it.isJsonNull }?.asString,
                    gameId = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    gameName = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    profileImageURL = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("profileImageURL")?.takeIf { !it.isJsonNull }?.asString,
                    tags = tags
                ))
            }
        }
        return ChannelVideosDataResponse(data, cursor, hasNextPage)
    }
}
