package com.github.andreyasadchy.xtra.model.gql.followed

import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class FollowedStreamsDataDeserializer : JsonDeserializer<FollowedStreamsDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FollowedStreamsDataResponse {
        val data = mutableListOf<Stream>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("currentUser")?.getAsJsonObject("followedLiveUsers")?.getAsJsonArray("edges")
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.takeIf { !it.isJsonNull }?.asString
        val hasNextPage = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("currentUser")?.getAsJsonObject("followedLiveUsers")?.get("pageInfo")?.takeIf { !it.isJsonNull }?.asJsonObject?.get("hasNextPage")?.takeIf { !it.isJsonNull }?.asBoolean
        dataJson?.forEach { item ->
            item?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
                val tags = mutableListOf<Tag>()
                obj.get("tags")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { tagElement ->
                    tagElement?.takeIf { it.isJsonObject }?.asJsonObject.let { tag ->
                        tags.add(Tag(
                            id = tag?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                            name = tag?.get("localizedName")?.takeIf { !it.isJsonNull }?.asString,
                        ))
                    }
                }
                data.add(Stream(
                    id = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    user_id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    user_login = obj.get("login")?.takeIf { !it.isJsonNull }?.asString,
                    user_name = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    game_id = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    game_name = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    type = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("type")?.takeIf { !it.isJsonNull }?.asString,
                    title = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("title")?.takeIf { !it.isJsonNull }?.asString,
                    viewer_count = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("viewersCount")?.takeIf { !it.isJsonNull }?.asInt,
                    thumbnail_url = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("previewImageURL")?.takeIf { !it.isJsonNull }?.asString,
                    profileImageURL = obj.get("profileImageURL")?.takeIf { !it.isJsonNull }?.asString,
                    tags = tags
                ))
            }
        }
        return FollowedStreamsDataResponse(data, cursor, hasNextPage)
    }
}
