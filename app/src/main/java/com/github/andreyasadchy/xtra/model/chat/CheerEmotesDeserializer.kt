package com.github.andreyasadchy.xtra.model.chat

import com.github.andreyasadchy.xtra.ui.view.chat.animateGifs
import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class CheerEmotesDeserializer : JsonDeserializer<CheerEmotesResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): CheerEmotesResponse {
        val emotes = mutableListOf<CheerEmote>()
        val dataJson = json.asJsonObject?.getAsJsonArray("data")
        dataJson?.forEach { set ->
            val name = set.asJsonObject.get("prefix").asString
            set.asJsonObject?.getAsJsonArray("tiers")?.forEach { emote ->
                emote.asJsonObject?.let { obj ->
                    val images = obj.get("images").asJsonObject.get("dark").asJsonObject
                    val animated = animateGifs && images.toString().contains("animated")
                    val urls = images.get(if (animated) "animated" else "static").asJsonObject
                    val url = urls.get(when (emoteQuality) {"4" -> ("4") "3" -> ("3") "2" -> ("2") else -> ("1")}).takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("3").takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("2").takeUnless { it?.isJsonNull == true }?.asString ?: urls.get("1").asString
                    emotes.add(CheerEmote(
                        name = name,
                        url = url,
                        type = if (animated) "image/gif" else null,
                        minBits = obj.get("min_bits").asInt,
                        color = obj.get("color")?.takeIf { !it.isJsonNull }?.asString
                    ))
                }
            }
        }
        return CheerEmotesResponse(emotes)
    }
}
