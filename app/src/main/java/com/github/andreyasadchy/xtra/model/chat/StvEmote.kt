package com.github.andreyasadchy.xtra.model.chat

class StvEmote(
    override val name: String,
    override val isPng: String,
    override val url: String,
    override val zerowidth: Boolean) : Emote()