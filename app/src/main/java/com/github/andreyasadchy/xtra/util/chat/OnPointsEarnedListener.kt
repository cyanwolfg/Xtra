package com.github.andreyasadchy.xtra.util.chat

interface OnPointsEarnedListener {
    fun onPointsEarned(message: PointsEarned)
}

data class PointsEarned(
    val pointsGained: Int? = null,
    val timestamp: Long? = null,
    val fullMsg: String? = null)
