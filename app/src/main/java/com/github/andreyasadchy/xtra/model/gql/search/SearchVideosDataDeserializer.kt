package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SearchVideosDataDeserializer : JsonDeserializer<SearchVideosDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SearchVideosDataResponse {
        val data = mutableListOf<Video>()
        var cursor: String? = null
        val dataJson = json.asJsonObject.getAsJsonObject("data").getAsJsonObject("searchFor").getAsJsonObject("videos").getAsJsonArray("edges")
        dataJson.forEach {
            cursor = if (!json.asJsonObject.getAsJsonObject("data").getAsJsonObject("searchFor").getAsJsonObject("videos").get("cursor").isJsonNull) json.asJsonObject.getAsJsonObject("data").getAsJsonObject("searchFor").getAsJsonObject("videos").getAsJsonPrimitive("cursor").asString else null
            val obj = it.asJsonObject.getAsJsonObject("item")
            data.add(Video(
                id = if (!(obj.get("id").isJsonNull)) { obj.getAsJsonPrimitive("id").asString } else "",
                user_id = if (!(obj.get("owner").isJsonNull)) { obj.getAsJsonObject("owner").getAsJsonPrimitive("id").asString } else null,
                user_login = if (!(obj.get("owner").isJsonNull)) { obj.getAsJsonObject("owner").getAsJsonPrimitive("login").asString } else null,
                user_name = if (!(obj.get("owner").isJsonNull)) { obj.getAsJsonObject("owner").getAsJsonPrimitive("displayName").asString } else null,
                title = if (!(obj.get("title").isJsonNull)) { obj.getAsJsonPrimitive("title").asString } else null,
                createdAt = if (!(obj.get("createdAt").isJsonNull)) { obj.getAsJsonPrimitive("createdAt").asString } else null,
                thumbnail_url = if (!(obj.get("previewThumbnailURL").isJsonNull)) { obj.getAsJsonPrimitive("previewThumbnailURL").asString } else null,
                view_count = if (!(obj.get("viewCount").isJsonNull)) { obj.getAsJsonPrimitive("viewCount").asInt } else null,
                duration = if (!(obj.get("lengthSeconds").isJsonNull)) { obj.getAsJsonPrimitive("lengthSeconds").asString } else null,
                gameId = if (!(obj.get("game").isJsonNull)) { obj.getAsJsonObject("game").getAsJsonPrimitive("id").asString } else null,
                gameName = if (!(obj.get("game").isJsonNull)) { obj.getAsJsonObject("game").getAsJsonPrimitive("displayName").asString } else null,
                tags = if (!(obj.get("contentTags").isJsonNull)) {
                    val tags = mutableListOf<Tag>()
                    obj.getAsJsonArray("contentTags").forEach { tag ->
                        tags.add(Tag(
                            id = tag.asJsonObject.getAsJsonPrimitive("id").asString,
                            name = tag.asJsonObject.getAsJsonPrimitive("localizedName").asString
                        ))
                    }
                    tags.ifEmpty { null }
                } else null
            ))
        }
        return SearchVideosDataResponse(data, cursor)
    }
}
