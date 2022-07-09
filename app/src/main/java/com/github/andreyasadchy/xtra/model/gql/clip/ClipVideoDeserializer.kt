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
        val data = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("clip")?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            Clip(
                id = "",
                video_id = obj.get("video")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { !it.isJsonNull }?.asString,
                duration = obj.get("durationSeconds")?.takeIf { !it.isJsonNull }?.asDouble,
                videoOffsetSeconds = obj.get("videoOffsetSeconds")?.takeIf { !it.isJsonNull }?.asInt,
            )
        }
        return ClipVideoResponse(data)
    }
}
