package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SearchChannelDataDeserializer : JsonDeserializer<SearchChannelDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SearchChannelDataResponse {
        val data = mutableListOf<ChannelSearch>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("searchFor")?.getAsJsonObject("channels")
        val cursor = dataJson?.getAsJsonPrimitive("cursor")?.asString
        dataJson?.getAsJsonArray("edges")?.forEach {
            it?.asJsonObject?.getAsJsonObject("item")?.let { obj ->
                data.add(ChannelSearch(
                    id = obj.getAsJsonPrimitive("id")?.asString,
                    broadcaster_login = obj.getAsJsonPrimitive("login")?.asString,
                    display_name = obj.getAsJsonPrimitive("displayName")?.asString,
                    is_live = obj.get("stream")?.isJsonObject,
                    profileImageURL = obj.getAsJsonPrimitive("profileImageURL")?.asString,
                    followers_count = obj.getAsJsonObject("followers")?.getAsJsonPrimitive("totalCount")?.asInt,
                    type = obj.getAsJsonObject("stream")?.getAsJsonPrimitive("type")?.asString
                ))
            }
        }
        return SearchChannelDataResponse(data, cursor)
    }
}
