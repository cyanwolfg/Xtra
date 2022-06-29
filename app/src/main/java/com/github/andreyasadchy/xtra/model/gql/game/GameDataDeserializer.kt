package com.github.andreyasadchy.xtra.model.gql.game

import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class GameDataDeserializer : JsonDeserializer<GameDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GameDataResponse {
        val data = mutableListOf<Game>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("directoriesWithTags")?.getAsJsonArray("edges")
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
                data.add(Game(
                    id = obj.getAsJsonPrimitive("id")?.asString,
                    name = obj.getAsJsonPrimitive("displayName")?.asString,
                    box_art_url = obj.getAsJsonPrimitive("avatarURL")?.asString,
                    viewersCount = obj.getAsJsonPrimitive("viewersCount")?.asInt,
                    tags = tags
                ))
            }
        }
        return GameDataResponse(data, cursor)
    }
}
