package com.github.andreyasadchy.xtra.model.chat

import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class StvEmotesDeserializer : JsonDeserializer<StvEmotesResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): StvEmotesResponse {
        val emotes = mutableListOf<StvEmote>()
        json.asJsonArray?.forEach { emote ->
            emote.asJsonObject?.let { obj ->
                obj.get("name")?.asString?.let { name ->
                    val urls = obj.getAsJsonArray("urls")
                    val url = urls.get(when (emoteQuality) {"4" -> 3 "3" -> 2 "2" -> 1 else -> 0})?.asJsonArray?.get(1)?.takeUnless { it.isJsonNull }?.asString ?: urls.get(2)?.asJsonArray?.get(1)?.takeUnless { it.isJsonNull }?.asString ?: urls.get(1)?.asJsonArray?.get(1)?.takeUnless { it.isJsonNull }?.asString ?: urls.get(0)?.asJsonArray?.get(1)?.asString
                    url?.let {
                        emotes.add(StvEmote(
                            name = name,
                            url = url,
                            type = obj.get("mime")?.takeIf { !it.isJsonNull }?.asString,
                            isZeroWidth = obj.get("visibility_simple")?.takeIf { it.isJsonArray }?.asJsonArray?.toString()?.contains("ZERO_WIDTH") == true
                        ))
                    }
                }
            }
        }
        return StvEmotesResponse(emotes)
    }
}
