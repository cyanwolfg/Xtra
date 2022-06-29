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
        dataJson?.forEach {
            it.asJsonObject.getAsJsonObject("node")?.let { obj ->
                data.add(Game(
                    id = obj.getAsJsonObject("details")?.getAsJsonObject("game")?.getAsJsonPrimitive("id")?.asString,
                    name = obj.getAsJsonObject("details")?.getAsJsonObject("game")?.getAsJsonPrimitive("displayName")?.asString,
                    box_art_url = obj.getAsJsonObject("details")?.getAsJsonObject("game")?.getAsJsonPrimitive("boxArtURL")?.asString,
                    vodPosition = obj.getAsJsonPrimitive("positionMilliseconds")?.asInt,
                    vodDuration = obj.getAsJsonPrimitive("durationMilliseconds")?.asInt,
                ))
            }
        }
        return VodGamesDataResponse(data)
    }
}
