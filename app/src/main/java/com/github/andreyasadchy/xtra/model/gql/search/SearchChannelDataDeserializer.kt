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
        val cursor = dataJson?.get("cursor")?.takeIf { !it.isJsonNull }?.asString
        dataJson?.getAsJsonArray("edges")?.forEach { item ->
            item?.asJsonObject?.getAsJsonObject("item")?.let { obj ->
                data.add(ChannelSearch(
                    id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
                    broadcaster_login = obj.get("login")?.takeIf { !it.isJsonNull }?.asString,
                    display_name = obj.get("displayName")?.takeIf { !it.isJsonNull }?.asString,
                    is_live = obj.get("stream")?.isJsonObject,
                    thumbnail_url = obj.get("profileImageURL")?.takeIf { !it.isJsonNull }?.asString,
                    followers_count = obj.get("followers")?.takeIf { it.isJsonObject }?.asJsonObject?.get("totalCount")?.takeIf { !it.isJsonNull }?.asInt,
                    type = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("type")?.takeIf { !it.isJsonNull }?.asString,
                ))
            }
        }
        return SearchChannelDataResponse(data, cursor)
    }
}
