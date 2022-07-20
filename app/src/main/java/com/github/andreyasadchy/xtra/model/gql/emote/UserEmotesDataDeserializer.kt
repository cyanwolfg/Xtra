package com.github.andreyasadchy.xtra.model.gql.emote

import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class UserEmotesDataDeserializer : JsonDeserializer<UserEmotesDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UserEmotesDataResponse {
        val data = mutableListOf<TwitchEmote>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("channel")?.getAsJsonObject("self")?.getAsJsonArray("availableEmoteSets")
        dataJson?.forEach { set ->
            set?.asJsonObject?.getAsJsonArray("emotes")?.forEach { emote ->
                emote?.asJsonObject?.let { obj ->
                    obj.get("id")?.takeIf { !it.isJsonNull }?.asString?.let { name ->
                        data.add(TwitchEmote(
                            name = name,
                            setId = obj.get("setID")?.takeIf { !it.isJsonNull }?.asString,
                            ownerId = set.asJsonObject?.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString
                        ))
                    }
                }
            }
        }
        return UserEmotesDataResponse(data)
    }
}
