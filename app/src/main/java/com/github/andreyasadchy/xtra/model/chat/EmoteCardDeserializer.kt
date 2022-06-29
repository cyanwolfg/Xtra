package com.github.andreyasadchy.xtra.model.chat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class EmoteCardDeserializer : JsonDeserializer<EmoteCardResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmoteCardResponse {
        val data = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("emote")?.let { obj ->
            EmoteCard(
                id = obj.getAsJsonPrimitive("id")?.asString,
                name = obj.getAsJsonPrimitive("token")?.asString,
                type = obj.getAsJsonPrimitive("type")?.asString,
                subTier = obj.getAsJsonPrimitive("subscriptionTier")?.asString,
                bitThreshold = obj.getAsJsonObject("bitsBadgeTierSummary")?.getAsJsonPrimitive("threshold")?.asInt,
                channelId = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("id")?.asString,
                channelLogin = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("login")?.asString,
                channelName = obj.getAsJsonObject("owner")?.getAsJsonPrimitive("displayName")?.asString
            )
        }
        return EmoteCardResponse(data)
    }
}
