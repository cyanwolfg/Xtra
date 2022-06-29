package com.github.andreyasadchy.xtra.model.gql.clip

data class ClipUrlsResponse(val data: List<ClipInfo>) {

    data class ClipInfo(
        val frameRate: Int?,
        val quality: String?,
        val url: String)
}