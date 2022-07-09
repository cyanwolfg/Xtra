package com.github.andreyasadchy.xtra.model.gql.playlist

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class VideoPlaylistTokenDeserializer : JsonDeserializer<VideoPlaylistTokenResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): VideoPlaylistTokenResponse {
        val tokenJson = json.takeIf { it.isJsonArray }?.asJsonArray?.first()?.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("videoPlaybackAccessToken")?.takeIf { it.isJsonObject }?.asJsonObject
        return VideoPlaylistTokenResponse(
            token = tokenJson?.get("value")?.takeIf { !it.isJsonNull }?.asString,
            signature = tokenJson?.get("signature")?.takeIf { !it.isJsonNull }?.asString
        )
    }
}