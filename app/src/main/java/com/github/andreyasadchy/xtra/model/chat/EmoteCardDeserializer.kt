package com.github.andreyasadchy.xtra.model.chat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class EmoteCardDeserializer : JsonDeserializer<EmoteCardResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmoteCardResponse {
        val data = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("emote")?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            EmoteCard(
                id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                name = obj.get("token")?.takeIf { !it.isJsonNull }?.asString,
                type = obj.get("type")?.takeIf { !it.isJsonNull }?.asString,
                subTier = obj.get("subscriptionTier")?.takeIf { !it.isJsonNull }?.asString,
                bitThreshold = obj.get("bitsBadgeTierSummary")?.takeIf { it.isJsonObject }?.asJsonObject?.get("threshold")?.takeIf { !it.isJsonNull }?.asInt,
                channelId = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                channelLogin = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("login")?.takeIf { !it.isJsonNull }?.asString,
                channelName = obj.get("owner")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { !it.isJsonNull }?.asString
            )
        }
        return EmoteCardResponse(data)
    }
}
