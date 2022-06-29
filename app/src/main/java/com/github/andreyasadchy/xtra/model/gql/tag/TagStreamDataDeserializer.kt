package com.github.andreyasadchy.xtra.model.gql.tag

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class TagStreamDataDeserializer : JsonDeserializer<TagStreamDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TagStreamDataResponse {
        val data = mutableListOf<Tag>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonArray("topTags")
        dataJson?.forEach {
            it?.asJsonObject?.let { obj ->
                data.add(Tag(
                    id = obj.getAsJsonPrimitive("id")?.asString,
                    name = obj.getAsJsonPrimitive("localizedName")?.asString,
                    scope = obj.getAsJsonPrimitive("scope")?.asString,
                ))
            }
        }
        return TagStreamDataResponse(data)
    }
}
