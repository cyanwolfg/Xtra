package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SearchGameDataDeserializer : JsonDeserializer<SearchGameDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SearchGameDataResponse {
        val data = mutableListOf<Game>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("searchFor")?.getAsJsonObject("games")
        val cursor = dataJson?.getAsJsonPrimitive("cursor")?.asString
        dataJson?.getAsJsonArray("edges")?.forEach {
            it?.asJsonObject?.getAsJsonObject("item")?.let { obj ->
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
                    box_art_url = obj.getAsJsonPrimitive("boxArtURL")?.asString,
                    viewersCount = obj.getAsJsonPrimitive("viewersCount")?.asInt ?: 0, // returns null if 0
                    tags = tags
                ))
            }
        }
        return SearchGameDataResponse(data, cursor)
    }
}
