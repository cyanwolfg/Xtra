package com.github.andreyasadchy.xtra.model.chat

import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class BttvFfzDeserializer : JsonDeserializer<BttvFfzResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BttvFfzResponse {
        val emotes = mutableListOf<FfzEmote>()
        json.asJsonArray?.forEach { emote ->
            emote.asJsonObject?.let { obj ->
                obj.get("code")?.asString?.let { name ->
                    val urls = obj.getAsJsonObject("images")
                    val url = urls.get(when (emoteQuality) {"4" -> ("4x") "3" -> ("2x") "2" -> ("2x") else -> ("1x")}).takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("2x").takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("1x").asString
                    url?.let {
                        emotes.add(FfzEmote(
                            name = name,
                            url = url,
                            type = obj.get("imageType")?.takeIf { !it.isJsonNull }?.asString?.let { type -> "image/$type" }
                        ))
                    }
                }
            }
        }
        return BttvFfzResponse(emotes)
    }
}
