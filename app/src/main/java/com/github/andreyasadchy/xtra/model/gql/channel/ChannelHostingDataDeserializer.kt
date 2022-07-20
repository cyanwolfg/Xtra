package com.github.andreyasadchy.xtra.model.gql.channel

import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ChannelHostingDataDeserializer : JsonDeserializer<ChannelHostingDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChannelHostingDataResponse {
        val data = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("hosting")?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            Stream(
                user_id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                user_login = obj.get("login")?.takeIf { !it.isJsonNull }?.asString,
                user_name = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString
            )
        }
        return ChannelHostingDataResponse(data)
    }
}
