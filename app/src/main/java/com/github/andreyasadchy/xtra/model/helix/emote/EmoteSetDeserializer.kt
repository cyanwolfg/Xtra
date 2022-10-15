package com.github.andreyasadchy.xtra.model.helix.emote

import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import kotlin.math.min

class EmoteSetDeserializer : JsonDeserializer<EmoteSetResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmoteSetResponse {
        val data = mutableListOf<TwitchEmote>()
        val dataJson = json.asJsonObject.getAsJsonArray("data")
        if (dataJson != null && dataJson.size() > 0) {
            val template = json.asJsonObject.get("template").asString
            val preferredScaleIndex = when (emoteQuality) {
                "4", "3" -> 2
                "2" -> 1
                else -> 0
            }
            dataJson.forEach { emote ->
                val obj = emote.asJsonObject
                val id = obj.get("id").asString
                val format = obj.getAsJsonArray("format").last().asString
                val theme = obj.getAsJsonArray("theme_mode").last().asString
                val scale = obj.getAsJsonArray("scale").let { it.get(min(preferredScaleIndex, it.size() - 1)) }.asString
                val url = template
                    .replaceFirst("{{id}}", id)
                    .replaceFirst("{{format}}", format)
                    .replaceFirst("{{theme_mode}}", theme)
                    .replaceFirst("{{scale}}", scale)
                data.add(
                    TwitchEmote(
                        name = obj.get("name").asString,
                        type = if (format == "animated") "image/gif" else null,
                        url = url,
                        setId = obj.get("emote_set_id").asString,
                        ownerId = obj.get("owner_id").asString
                    )
                )
            }
        }
        return EmoteSetResponse(data.sortedByDescending { it.setId })
    }
}