package com.github.andreyasadchy.xtra.model.chat

import com.google.gson.annotations.SerializedName

data class VideoChatMessage(
        @SerializedName("_id")
        override val id: String?,
        @SerializedName("content_offset_seconds")
        val contentOffsetSeconds: Double,
        val commenter: Commenter?,
        @SerializedName("message")
        val messageObj: Message?) : ChatMessage {

    override val userId: String?
        get() = commenter?.id

    override val userLogin: String?
        get() = commenter?.name

    override val userName: String?
        get() = commenter?.displayName

    override val message: String?
        get() = messageObj?.body

    override val color: String?
        get() = messageObj?.userColor

    override val isAction: Boolean
        get() = messageObj?.isAction ?: false

    override val emotes: List<TwitchEmote>?
        get() = messageObj?.emoticons

    override val badges: List<Badge>?
        get() = messageObj?.userBadges

    override val fullMsg: String
        get() = this.toString()

    val msgId: String?
        get() = messageObj?.userNotice?.msgId

    data class Commenter(
            @SerializedName("display_name")
            val displayName: String?,
            @SerializedName("_id")
            val id: String?,
            val name: String?)

    data class Message(
            val body: String?,
            val emoticons: List<TwitchEmote>?,
            @SerializedName("is_action")
            val isAction: Boolean?,
            @SerializedName("user_badges")
            val userBadges: List<Badge>?,
            @SerializedName("user_color")
            val userColor: String?,
            @SerializedName("user_notice_params")
            val userNotice: UserNotice?)

    data class UserNotice(
            @SerializedName("msg-id")
            val msgId: String?)
}