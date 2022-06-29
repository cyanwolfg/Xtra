package com.github.andreyasadchy.xtra.model.gql.followed

import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class FollowedChannelsDataDeserializer : JsonDeserializer<FollowedChannelsDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FollowedChannelsDataResponse {
        val data = mutableListOf<Follow>()
        val dataJson = json.asJsonObject?.getAsJsonObject("data")?.getAsJsonObject("user")?.getAsJsonObject("follows")?.getAsJsonArray("edges")
        val cursor = dataJson?.lastOrNull()?.asJsonObject?.get("cursor")?.asString
        dataJson?.forEach {
            it?.asJsonObject?.getAsJsonObject("node")?.let { obj ->
                data.add(Follow(
                    to_id = obj.getAsJsonPrimitive("id")?.asString,
                    to_login = obj.getAsJsonPrimitive("login")?.asString,
                    to_name = obj.getAsJsonPrimitive("displayName")?.asString,
                    followed_at = obj.getAsJsonObject("self")?.getAsJsonObject("follower")?.getAsJsonPrimitive("followedAt")?.asString,
                    profileImageURL = obj.getAsJsonPrimitive("profileImageURL")?.asString,
                ))
            }
        }
        return FollowedChannelsDataResponse(data, cursor)
    }
}
