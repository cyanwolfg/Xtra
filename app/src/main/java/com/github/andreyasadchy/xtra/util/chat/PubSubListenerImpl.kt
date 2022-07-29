package com.github.andreyasadchy.xtra.util.chat

import com.github.andreyasadchy.xtra.model.chat.PubSubPointReward
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import org.json.JSONObject

class PubSubListenerImpl(
    private val callback: OnChatMessageReceivedListener,
    private val callbackReward: OnRewardReceivedListener,
    private val callbackPointsEarned: OnPointsEarnedListener,
    private val callbackClaim: OnClaimPointsListener,
    private val callbackMinute: OnMinuteWatchedListener,
    private val callbackRaid: OnRaidListener,
    private val callbackViewers: OnViewerCountReceivedListener) : PubSubWebSocket.OnMessageReceivedListener {

    override fun onPlaybackMessage(text: String) {
        val data = if (text.isNotBlank()) JSONObject(text).optJSONObject("data") else null
        val message = data?.optString("message")?.let { if (it.isNotBlank() && !data.isNull("message")) JSONObject(it) else null }
        callbackViewers.onViewerCount(if (message?.isNull("viewers") == false) message.optInt("viewers") else null)
    }

    override fun onPointReward(text: String) {
        val data = if (text.isNotBlank()) JSONObject(text).optJSONObject("data") else null
        val message = data?.optString("message")?.let { if (it.isNotBlank() && !data.isNull("message")) JSONObject(it) else null }
        val messageData = message?.optString("data")?.let { if (it.isNotBlank() && !message.isNull("data")) JSONObject(it) else null }
        val redemption = messageData?.optString("redemption")?.let { if (it.isNotBlank() && !messageData.isNull("redemption")) JSONObject(it) else null }
        val user = redemption?.optString("user")?.let { if (it.isNotBlank() && !redemption.isNull("user")) JSONObject(it) else null }
        val reward = redemption?.optString("reward")?.let { if (it.isNotBlank() && !redemption.isNull("reward")) JSONObject(it) else null }
        val rewardImage = reward?.optString("image")?.let { if (it.isNotBlank() && !reward.isNull("image")) JSONObject(it) else null }
        val defaultImage = reward?.optString("default_image")?.let { if (it.isNotBlank() && !reward.isNull("default_image")) JSONObject(it) else null }
        val input = redemption?.optString("user_input")
        val pointReward = PubSubPointReward(
            id = reward?.optString("id"),
            userId = user?.optString("id"),
            userLogin = user?.optString("login"),
            userName = user?.optString("display_name"),
            message = input,
            fullMsg = text,
            rewardTitle = reward?.optString("title"),
            rewardCost = reward?.optInt("cost"),
            rewardImage = PubSubPointReward.RewardImage(
                url1 = rewardImage?.optString("url_1x") ?: defaultImage?.optString("url_1x"),
                url2 = rewardImage?.optString("url_2x") ?: defaultImage?.optString("url_2x"),
                url4 = rewardImage?.optString("url_4x") ?: defaultImage?.optString("url_4x"),
            ),
            timestamp = messageData?.optString("timestamp")?.let { TwitchApiHelper.parseIso8601Date(it) }
        )
        if (input.isNullOrBlank()) {
            callback.onMessage(pointReward)
        } else {
            callbackReward.onReward(pointReward)
        }
    }

    override fun onPointsEarned(text: String) {
        val data = if (text.isNotBlank()) JSONObject(text).optJSONObject("data") else null
        val message = data?.optString("message")?.let { if (it.isNotBlank() && !data.isNull("message")) JSONObject(it) else null }
        val messageData = message?.optString("data")?.let { if (it.isNotBlank() && !message.isNull("data")) JSONObject(it) else null }
        val pointGain = messageData?.optString("point_gain")?.let { if (it.isNotBlank() && !messageData.isNull("point_gain")) JSONObject(it) else null }
        callbackPointsEarned.onPointsEarned(PointsEarned(
            pointsGained = pointGain?.optInt("total_points"),
            timestamp = messageData?.optString("timestamp")?.let { TwitchApiHelper.parseIso8601Date(it) },
            fullMsg = text
        ))
    }

    override fun onClaimPoints(text: String) {
        val data = if (text.isNotBlank()) JSONObject(text).optJSONObject("data") else null
        val message = data?.optString("message")?.let { if (it.isNotBlank() && !data.isNull("message")) JSONObject(it) else null }
        val messageData = message?.optString("data")?.let { if (it.isNotBlank() && !message.isNull("data")) JSONObject(it) else null }
        val claim = messageData?.optString("claim")?.let { if (it.isNotBlank() && !messageData.isNull("claim")) JSONObject(it) else null }
        callbackClaim.onClaim(Claim(
            claimId = claim?.optString("id"),
            channelId = claim?.optString("channel_id")
        ))
    }

    override fun onMinuteWatched() {
        callbackMinute.onMinuteWatched()
    }

    override fun onRaidUpdate(text: String, openStream: Boolean) {
        val data = if (text.isNotBlank()) JSONObject(text).optJSONObject("data") else null
        val message = data?.optString("message")?.let { if (it.isNotBlank() && !data.isNull("message")) JSONObject(it) else null }
        val raid = message?.optString("raid")?.let { if (it.isNotBlank() && !message.isNull("raid")) JSONObject(it) else null }
        callbackRaid.onRaidUpdate(Raid(
            raidId = raid?.optString("id"),
            targetId = raid?.optString("target_id"),
            targetLogin = raid?.optString("target_login"),
            targetName = raid?.optString("target_display_name"),
            targetProfileImage = raid?.optString("target_profile_image"),
            viewerCount = raid?.optInt("viewer_count"),
            openStream = openStream
        ))
    }
}
