package com.github.andreyasadchy.xtra.model.gql.followed

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class FollowUserDataDeserializer : JsonDeserializer<FollowUserDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FollowUserDataResponse {
        val error = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("followUser")?.takeIf { it.isJsonObject }?.asJsonObject?.get("error")?.takeIf { !it.isJsonNull }?.asString
        return FollowUserDataResponse(error)
    }
}
