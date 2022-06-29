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
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("channel")?.getAsJsonObject("chatters")
        dataJson?.getAsJsonArray("broadcasters")?.forEach {
            it.asJsonObject?.let { obj ->
                obj.getAsJsonPrimitive("login")?.asString?.let { login -> broadcasters.add(login) }
            }
        }
        dataJson?.getAsJsonArray("moderators")?.forEach {
            it.asJsonObject?.let { obj ->
                obj.getAsJsonPrimitive("login")?.asString?.let { login -> moderators.add(login) }
            }
        }
        dataJson?.getAsJsonArray("vips")?.forEach {
            it.asJsonObject?.let { obj ->
                obj.getAsJsonPrimitive("login")?.asString?.let { login -> vips.add(login) }
            }
        }
        dataJson?.getAsJsonArray("viewers")?.forEach {
            it.asJsonObject?.let { obj ->
                obj.getAsJsonPrimitive("login")?.asString?.let { login -> viewers.add(login) }
            }
        }
        val count = dataJson?.getAsJsonPrimitive("count")?.asInt
        return ChannelViewerListDataResponse(ChannelViewerList(broadcasters, moderators, vips, viewers, count))
    }
}
