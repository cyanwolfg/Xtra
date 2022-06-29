package com.github.andreyasadchy.xtra.model.helix.emote

import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class EmoteSetDeserializer : JsonDeserializer<EmoteSetResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmoteSetResponse {
        val data = mutableListOf<TwitchEmote>()
        val dataJson = json.asJsonObject?.getAsJsonArray("data")
        dataJson?.forEach { emote ->
            emote?.asJsonObject?.let { obj ->
                obj.get("name")?.asString?.let { name ->
                    val urls = obj.getAsJsonObject("images")
                    val url = urls.get(when (emoteQuality) {"4" -> ("url_4x") "3" -> ("url_4x") "2" -> ("url_2x") else -> ("url_1x")}).takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("url_2x").takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("url_1x").asString
                    url?.let {
                        val format = if (obj.getAsJsonArray("format")?.first()?.asString.equals("animated")) "image/gif" else null
                        data.add(TwitchEmote(
                            name = name,
                            type = format,
                            url = url,
                            setId = obj.get("emote_set_id")?.asString,
                            ownerId = obj.get("owner_id")?.asString
                        ))
                    }
                }
            }
        }
        return EmoteSetResponse(data.sortedByDescending { it.setId })
    }
}