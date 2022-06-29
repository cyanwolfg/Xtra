package com.github.andreyasadchy.xtra.model.chat

import com.github.andreyasadchy.xtra.ui.view.chat.emoteQuality

class BttvEmote(
        override val name: String,
        override val type: String?,
        val id: String) : Emote() {

        override val url: String
                get() = "https://cdn.betterttv.net/emote/$id/${(when (emoteQuality) {"4" -> ("3x") "3" -> ("2x") "2" -> ("2x") else -> ("1x")})}"

        private val zeroWidthList = listOf("IceCold", "SoSnowy", "SantaHat", "TopHat", "CandyCane", "ReinDeer", "cvHazmat", "cvMask")
        override val isZeroWidth: Boolean = zeroWidthList.contains(name)
}