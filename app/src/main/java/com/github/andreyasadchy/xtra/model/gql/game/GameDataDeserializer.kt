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
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.takeIf { !it.isJsonNull }?.asString
        val hasNextPage = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("directoriesWithTags")?.get("pageInfo")?.takeIf { !it.isJsonNull }?.asJsonObject?.get("hasNextPage")?.takeIf { !it.isJsonNull }?.asBoolean
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
                data.add(Game(
                    id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    name = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    box_art_url = obj.get("avatarURL")?.takeIf { !it.isJsonNull }?.asString,
                    viewersCount = obj.get("viewersCount")?.takeIf { !it.isJsonNull }?.asInt,
                    tags = tags
                ))
            }
        }
        return GameDataResponse(data, cursor, hasNextPage)
    }
}
