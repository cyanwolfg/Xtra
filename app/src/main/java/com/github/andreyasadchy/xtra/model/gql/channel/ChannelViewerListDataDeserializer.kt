package com.github.andreyasadchy.xtra.model.gql.channel

import com.github.andreyasadchy.xtra.model.helix.channel.ChannelViewerList
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class ChannelViewerListDataDeserializer : JsonDeserializer<ChannelViewerListDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChannelViewerListDataResponse {
        val broadcasters = mutableListOf<String>()
        val moderators = mutableListOf<String>()
        val vips = mutableListOf<String>()
        val viewers = mutableListOf<String>()
        val dataJson = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("channel")?.takeIf { it.isJsonObject }?.asJsonObject?.get("chatters")?.takeIf { it.isJsonObject }?.asJsonObject
        dataJson?.get("broadcasters")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                obj.get("login")?.takeIf { !it.isJsonNull }?.asString?.let { broadcasters.add(it) }
            }
        }
        dataJson?.get("moderators")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                obj.get("login")?.takeIf { !it.isJsonNull }?.asString?.let { moderators.add(it) }
            }
        }
        dataJson?.get("vips")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                obj.get("login")?.takeIf { !it.isJsonNull }?.asString?.let { vips.add(it) }
            }
        }
        dataJson?.get("viewers")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                obj.get("login")?.takeIf { !it.isJsonNull }?.asString?.let { viewers.add(it) }
            }
        }
        val count = dataJson?.get("count")?.takeIf { !it.isJsonNull }?.asInt
        return ChannelViewerListDataResponse(ChannelViewerList(broadcasters, moderators, vips, viewers, count))
    }
}
