package com.github.andreyasadchy.xtra.model.gql.points

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ChannelPointsContextDataDeserializer : JsonDeserializer<ChannelPointsContextDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChannelPointsContextDataResponse {
        val obj = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("community")?.getAsJsonObject("channel")?.getAsJsonObject("self")?.getAsJsonObject("communityPoints")?.getAsJsonPrimitive("balance")?.asInt
        return ChannelPointsContextDataResponse(obj)
    }
}
