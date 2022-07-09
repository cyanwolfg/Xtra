package com.github.andreyasadchy.xtra.util.chat

interface OnClaimPointsListener {
    fun onClaim(message: Claim)
}

data class Claim(
    val claimId: String? = null,
    val channelId: String? = null)
