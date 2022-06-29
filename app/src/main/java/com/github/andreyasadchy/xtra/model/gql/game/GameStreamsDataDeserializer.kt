package com.github.andreyasadchy.xtra.model.gql.game

import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class GameStreamsDataDeserializer : JsonDeserializer<GameStreamsDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GameStreamsDataResponse {
        val data = mutableListOf<Stream>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("game")?.getAsJsonObject("streams")?.getAsJsonArray("edges")
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.asString
        dataJson?.forEach {
            it?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
                val tags = mutableListOf<Tag>()
                obj.getAsJsonArray("tags")?.forEach { tag ->
                    tags.add(Tag(
                        id = tag.asJsonObject?.getAsJsonPrimitive("id")?.asString,
                        name = tag.asJsonObject?.getAsJsonPrimitive("localizedName")?.asString
                    ))
                }
                data.add(Stream(
                    id = obj.getAsJsonPrimitive("id")?.asString,
                    user_id = obj.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("id")?.asString,
                    user_login = obj.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("login")?.asString,
                    user_name = obj.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("displayName")?.asString,
                    type = obj.getAsJsonPrimitive("type")?.asString,
                    title = obj.getAsJsonPrimitive("title")?.asString,
                    viewer_count = obj.getAsJsonPrimitive("viewersCount")?.asInt,
                    thumbnail_url = obj.getAsJsonPrimitive("previewImageURL")?.asString,
                    profileImageURL = obj.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("profileImageURL")?.asString,
                    tags = tags
                ))
            }
        }
        return GameStreamsDataResponse(data, cursor)
    }
}
