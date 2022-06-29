package com.github.andreyasadchy.xtra.model.chat

class CheerEmote(
    override val name: String,
    override val url: String,
    override val type: String?,
    val minBits: Int,
    val color: String? = null) : Emote()