package com.github.andreyasadchy.xtra.model.gql.vod

import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class VodGamesDataDeserializer : JsonDeserializer<VodGamesDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): VodGamesDataResponse {
        val data = mutableListOf<Game>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("video")?.getAsJsonObject("moments")?.getAsJsonArray("edges")
        dataJson?.forEach { item ->
            item.asJsonObject.getAsJsonObject("node")?.let { obj ->
                data.add(Game(
                    id = obj.get("details")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    name = obj.get("details")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    box_art_url = obj.get("details")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("boxArtURL")?.takeIf { !it.isJsonNull }?.asString,
                    vodPosition = obj.get("positionMilliseconds")?.takeIf { !it.isJsonNull }?.asInt,
                    vodDuration = obj.get("durationMilliseconds")?.takeIf { !it.isJsonNull }?.asInt,
                ))
            }
        }
        return VodGamesDataResponse(data)
    }
}
