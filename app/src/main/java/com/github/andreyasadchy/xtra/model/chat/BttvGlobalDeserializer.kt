package com.github.andreyasadchy.xtra.model.chat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class BttvGlobalDeserializer : JsonDeserializer<BttvGlobalResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BttvGlobalResponse {
        val emotes = mutableListOf<BttvEmote>()
        json.asJsonArray?.forEach { emote ->
            emote.asJsonObject?.let { obj ->
                obj.get("code")?.asString?.let { name ->
                    obj.get("id")?.asString?.let { id ->
                        emotes.add(BttvEmote(
                            id = id,
                            name = name,
                            type = obj.get("imageType")?.takeIf { !it.isJsonNull }?.asString?.let { type -> "image/$type" }
                        ))
                    }
                }
            }
        }
        return BttvGlobalResponse(emotes)
    }
}
