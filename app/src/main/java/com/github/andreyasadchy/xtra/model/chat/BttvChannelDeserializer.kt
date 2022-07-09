package com.github.andreyasadchy.xtra.model.chat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class BttvChannelDeserializer : JsonDeserializer<BttvChannelResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BttvChannelResponse {
        val emotes = mutableListOf<BttvEmote>()
        val dataJson = json.asJsonObject?.entrySet()
        dataJson?.forEach { set ->
            if (set.value?.isJsonArray == true && set.key != "bots") {
                set.value?.asJsonArray?.forEach { emote ->
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
            }
        }
        return BttvChannelResponse(emotes)
    }
}
