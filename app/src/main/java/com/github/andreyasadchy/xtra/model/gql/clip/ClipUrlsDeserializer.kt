package com.github.andreyasadchy.xtra.model.gql.clip

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ClipUrlsDeserializer : JsonDeserializer<ClipUrlsResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ClipUrlsResponse {
        val data = mutableListOf<ClipUrlsResponse.ClipInfo>()
        val dataJson = json.asJsonArray?.first()?.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("clip")?.getAsJsonArray("videoQualities")
        dataJson?.forEach { item ->
            item?.asJsonObject?.let { obj ->
                obj.get("sourceURL")?.takeIf { !it.isJsonNull }?.asString?.let { url ->
                    data.add(ClipUrlsResponse.ClipInfo(
                        frameRate = obj.get("frameRate")?.takeIf { !it.isJsonNull }?.asInt,
                        quality = obj.get("quality")?.takeIf { !it.isJsonNull }?.asString,
                        url = url.replace(Regex("https://[^/]+"),"https://clips-media-assets2.twitch.tv")
                    ))
                }
            }
        }
        return ClipUrlsResponse(data)
    }
}
