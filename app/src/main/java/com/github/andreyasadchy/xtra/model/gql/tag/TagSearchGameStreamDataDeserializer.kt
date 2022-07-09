package com.github.andreyasadchy.xtra.model.gql.tag

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class TagSearchGameStreamDataDeserializer : JsonDeserializer<TagSearchGameStreamDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TagSearchGameStreamDataResponse {
        val data = mutableListOf<Tag>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonArray("searchLiveTags")
        dataJson?.forEach { item ->
            item?.asJsonObject?.let { obj ->
                data.add(Tag(
                    id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    name = obj.get("localizedName")?.takeIf { !it.isJsonNull }?.asString,
                    scope = "ALL"
                ))
            }
        }
        return TagSearchGameStreamDataResponse(data)
    }
}
