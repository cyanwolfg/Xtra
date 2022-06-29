package com.github.andreyasadchy.xtra.model.gql.clip

import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ClipVideoDeserializer : JsonDeserializer<ClipVideoResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ClipVideoResponse {
        val obj = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("clip")
        val data = (Clip(
            id = "",
            video_id = obj?.getAsJsonObject("video")?.getAsJsonPrimitive("id")?.asString,
            duration = obj?.getAsJsonPrimitive("durationSeconds")?.asDouble,
            videoOffsetSeconds = obj?.getAsJsonPrimitive("videoOffsetSeconds")?.asInt,
        ))
        return ClipVideoResponse(data)
    }
}
