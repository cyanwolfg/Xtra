package com.github.andreyasadchy.xtra.model.gql.clip

import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ClipDataDeserializer : JsonDeserializer<ClipDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ClipDataResponse {
        val obj = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("clip")
        val data = (Clip(
            id = "",
            broadcaster_id = obj?.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("id")?.asString,
            broadcaster_login = obj?.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("login")?.asString,
            broadcaster_name = obj?.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("displayName")?.asString,
            profileImageURL = obj?.getAsJsonObject("broadcaster")?.getAsJsonPrimitive("profileImageURL")?.asString,
            videoOffsetSeconds = obj?.getAsJsonPrimitive("videoOffsetSeconds")?.asInt,
        ))
        return ClipDataResponse(data)
    }
}
