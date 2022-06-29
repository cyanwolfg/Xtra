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
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.asString
        dataJson?.forEach {
            it?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
                val tags = mutableListOf<Tag>()
                obj.getAsJsonObject("stream")?.getAsJsonArray("tags")?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.asJsonObject?.getAsJsonPrimitive("id")?.asString,
                        name = tag.asJsonObject?.getAsJsonPrimitive("localizedName")?.asString
                    ))
                }
                data.add(Stream(
                    id = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("id")?.asString,
                    user_id = obj.getAsJsonPrimitive("id")?.asString,
                    user_login = obj.getAsJsonPrimitive("login")?.asString,
                    user_name = obj.getAsJsonPrimitive("displayName")?.asString,
                    game_id = obj.getAsJsonObject("stream")?.getAsJsonObject("game")?.getAsJsonPrimitive("id")?.asString,
                    game_name = obj.getAsJsonObject("stream")?.getAsJsonObject("game")?.getAsJsonPrimitive("displayName")?.asString,
                    type = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("type")?.asString,
                    title = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("title")?.asString,
                    viewer_count = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("viewersCount")?.asInt,
                    thumbnail_url = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("previewImageURL")?.asString,
                    profileImageURL = obj.getAsJsonPrimitive("profileImageURL")?.asString,
                    tags = tags
                ))
            }
        }
        return FollowedStreamsDataResponse(data, cursor)
    }
}
