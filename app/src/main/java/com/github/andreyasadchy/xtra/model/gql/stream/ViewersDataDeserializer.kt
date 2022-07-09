package com.github.andreyasadchy.xtra.model.gql.stream

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ViewersDataDeserializer : JsonDeserializer<ViewersDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ViewersDataResponse {
        val obj = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("user")?.getAsJsonObject("stream")
        return ViewersDataResponse(
            streamId = obj?.get("id")?.takeIf { !it.isJsonNull }?.asString,
            viewers = obj?.getAsJsonPrimitive("viewersCount")?.asInt
        )
    }
}
