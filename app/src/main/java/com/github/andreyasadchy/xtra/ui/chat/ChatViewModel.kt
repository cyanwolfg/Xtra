package com.github.andreyasadchy.xtra.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.chat.*
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.BaseViewModel
import com.github.andreyasadchy.xtra.ui.player.ChatReplayManager
import com.github.andreyasadchy.xtra.ui.player.stream.stream_id
import com.github.andreyasadchy.xtra.ui.view.chat.ChatView
import com.github.andreyasadchy.xtra.ui.view.chat.MAX_ADAPTER_COUNT
import com.github.andreyasadchy.xtra.util.SingleLiveEvent
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.chat.*
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.asReversed
import kotlin.collections.associateBy
import kotlin.collections.chunked
import kotlin.collections.contains
import kotlin.collections.containsKey
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.hashSetOf
import kotlin.collections.isNotEmpty
import kotlin.collections.isNullOrEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.sortedBy

class ChatViewModel @Inject constructor(
        private val repository: TwitchService,
        private val playerRepository: PlayerRepository) : BaseViewModel(), ChatView.MessageSenderCallback {

    val recentEmotes: LiveData<List<Emote>> by lazy {
        MediatorLiveData<List<Emote>>().apply {
            addSource(userEmotes) { twitch ->
                removeSource(userEmotes)
                addSource(_otherEmotes) { other ->
                    removeSource(_otherEmotes)
                    addSource(playerRepository.loadRecentEmotes()) { recent ->
                        value = recent.filter { (twitch.contains<Emote>(it) || other.contains(it)) }
                    }
                }
            }
        }
    }
    private val _otherEmotes = MutableLiveData<List<Emote>>()
    val otherEmotes: LiveData<List<Emote>>
        get() = _otherEmotes

    val recentMessages = MutableLiveData<List<LiveChatMessage>>()
    val globalBadges = MutableLiveData<List<TwitchBadge>?>()
    val channelBadges = MutableLiveData<List<TwitchBadge>>()
    val cheerEmotes = MutableLiveData<List<CheerEmote>>()
    var emoteSetsAdded = false
    val userEmotes = MutableLiveData<List<Emote>>()
    val reloadMessages = MutableLiveData<Boolean>()
    val roomState = MutableLiveData<RoomState>()
    val command = MutableLiveData<Command>()
    val reward = MutableLiveData<ChatMessage>()
    val pointsEarned = MutableLiveData<PointsEarned>()
    var showRaids = false
    val raid = MutableLiveData<Raid>()
    val raidClicked = MutableLiveData<Boolean>()
    var raidAutoSwitch = false
    var raidNewId = true
    var raidClosed = false
    val host = MutableLiveData<Stream>()
    val hostClicked = MutableLiveData<Boolean>()
    val viewerCount = MutableLiveData<Int?>()

    private val _chatMessages by lazy {
        MutableLiveData<MutableList<ChatMessage>>().apply { value = Collections.synchronizedList(ArrayList(MAX_ADAPTER_COUNT + 1)) }
    }
    val chatMessages: LiveData<MutableList<ChatMessage>>
        get() = _chatMessages
    private val _newMessage by lazy { MutableLiveData<ChatMessage>() }
    val newMessage: LiveData<ChatMessage>
        get() = _newMessage

    var chat: ChatController? = null

    private val _newChatter by lazy { SingleLiveEvent<Chatter>() }
    val newChatter: LiveData<Chatter>
        get() = _newChatter

    val chatters: Collection<Chatter>?
        get() = (chat as? LiveChatController)?.chatters?.values

    fun startLive(useSSl: Boolean, usePubSub: Boolean, user: User, isLoggedIn: Boolean, helixClientId: String?, gqlClientId: String?, channelId: String?, channelLogin: String?, channelName: String?, streamId: String?, showUserNotice: Boolean, showClearMsg: Boolean, showClearChat: Boolean, collectPoints: Boolean, notifyPoints: Boolean, showRaids: Boolean, autoSwitchRaids: Boolean, enableRecentMsg: Boolean? = false, recentMsgLimit: String? = null) {
        if (chat == null && channelLogin != null) {
            stream_id = streamId
            this.showRaids = showRaids
            raidAutoSwitch = autoSwitchRaids
            chat = LiveChatController(
                useSSl = useSSl,
                usePubSub = usePubSub,
                user = user,
                isLoggedIn = isLoggedIn,
                helixClientId = helixClientId,
                gqlClientId = gqlClientId,
                channelId = channelId,
                channelLogin = channelLogin,
                displayName = channelName,
                showUserNotice = showUserNotice,
                showClearMsg = showClearMsg,
                showClearChat = showClearChat,
                collectPoints = collectPoints,
                notifyPoints = notifyPoints
            )
            init(
                helixClientId = helixClientId,
                helixToken = user.helixToken?.nullIfEmpty(),
                gqlClientId = gqlClientId,
                channelId = channelId,
                channelLogin = channelLogin,
                enableRecentMsg = enableRecentMsg,
                recentMsgLimit = recentMsgLimit
            )
        }
    }

    fun startReplay(user: User, helixClientId: String?, gqlClientId: String?, channelId: String?, videoId: String, startTime: Double, getCurrentPosition: () -> Double) {
        if (chat == null) {
            chat = VideoChatController(
                clientId = gqlClientId,
                videoId = videoId,
                startTime = startTime,
                getCurrentPosition = getCurrentPosition
            )
            init(
                helixClientId = helixClientId,
                helixToken = user.helixToken?.nullIfEmpty(),
                gqlClientId = gqlClientId,
                channelId = channelId
            )
        }
    }

    fun start() {
        chat?.start()
    }

    fun stop() {
        chat?.pause()
    }

    override fun send(message: CharSequence) {
        chat?.send(message)
    }

    override fun onCleared() {
        chat?.stop()
        super.onCleared()
    }

    private fun init(helixClientId: String?, helixToken: String?, gqlClientId: String?, channelId: String?, channelLogin: String? = null, enableRecentMsg: Boolean? = false, recentMsgLimit: String? = null) {
        chat?.start()
        loadEmotes(helixClientId, helixToken, gqlClientId, channelId)
        if (channelLogin != null && enableRecentMsg == true) {
            loadRecentMessages(channelLogin, recentMsgLimit)
        }
    }

    private fun loadEmotes(helixClientId: String?, helixToken: String?, gqlClientId: String?, channelId: String?) {
        val list = mutableListOf<Emote>()
        savedGlobalBadges.also {
            if (!it.isNullOrEmpty()) {
                globalBadges.value = it
                reloadMessages.value = true
            } else {
                viewModelScope.launch {
                    try {
                        playerRepository.loadGlobalBadges().body()?.badges?.let { badges ->
                            if (badges.isNotEmpty()) {
                                savedGlobalBadges = badges
                                globalBadges.value = badges
                                reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load global badges", e)
                    }
                }
            }
        }
        globalStvEmotes.also {
            if (!it.isNullOrEmpty()) {
                (chat as? LiveChatController)?.addEmotes(it)
                list.addAll(it)
                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                reloadMessages.value = true
            } else {
                viewModelScope.launch {
                    try {
                        playerRepository.loadGlobalStvEmotes().body()?.emotes?.let { emotes ->
                            if (emotes.isNotEmpty()) {
                                globalStvEmotes = emotes
                                (chat as? LiveChatController)?.addEmotes(emotes)
                                list.addAll(emotes)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load global 7tv emotes", e)
                    }
                }
            }
        }
        globalBttvEmotes.also {
            if (!it.isNullOrEmpty()) {
                (chat as? LiveChatController)?.addEmotes(it)
                list.addAll(it)
                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                reloadMessages.value = true
            } else {
                viewModelScope.launch {
                    try {
                        playerRepository.loadGlobalBttvEmotes().body()?.emotes?.let { emotes ->
                            if (emotes.isNotEmpty()) {
                                globalBttvEmotes = emotes
                                (chat as? LiveChatController)?.addEmotes(emotes)
                                list.addAll(emotes)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load global BTTV emotes", e)
                    }
                }
            }
        }
        globalFfzEmotes.also {
            if (!it.isNullOrEmpty()) {
                (chat as? LiveChatController)?.addEmotes(it)
                list.addAll(it)
                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                reloadMessages.value = true
            } else {
                viewModelScope.launch {
                    try {
                        playerRepository.loadBttvGlobalFfzEmotes().body()?.emotes?.let { emotes ->
                            if (emotes.isNotEmpty()) {
                                globalFfzEmotes = emotes
                                (chat as? LiveChatController)?.addEmotes(emotes)
                                list.addAll(emotes)
                                _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                                reloadMessages.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load global FFZ emotes", e)
                    }
                }
            }
        }
        if (!channelId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    playerRepository.loadChannelBadges(channelId).body()?.badges?.let {
                        if (it.isNotEmpty()) {
                            channelBadges.postValue(it)
                            reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load badges for channel $channelId", e)
                }
            }
            viewModelScope.launch {
                try {
                    playerRepository.loadStvEmotes(channelId).body()?.emotes?.let {
                        if (it.isNotEmpty()) {
                            (chat as? LiveChatController)?.addEmotes(it)
                            list.addAll(it)
                            _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                            reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load 7tv emotes for channel $channelId", e)
                }
            }
            viewModelScope.launch {
                try {
                    playerRepository.loadBttvEmotes(channelId).body()?.emotes?.let {
                        if (it.isNotEmpty()) {
                            (chat as? LiveChatController)?.addEmotes(it)
                            list.addAll(it)
                            _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                            reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load BTTV emotes for channel $channelId", e)
                }
            }
            viewModelScope.launch {
                try {
                    playerRepository.loadBttvFfzEmotes(channelId).body()?.emotes?.let {
                        if (it.isNotEmpty()) {
                            (chat as? LiveChatController)?.addEmotes(it)
                            list.addAll(it)
                            _otherEmotes.value = list.sortedBy { emote -> emote is StvEmote }.sortedBy { emote -> emote is BttvEmote }.sortedBy { emote -> emote is FfzEmote }
                            reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load FFZ emotes for channel $channelId", e)
                }
            }
            viewModelScope.launch {
                try {
                    repository.loadCheerEmotes(channelId, helixClientId, helixToken, gqlClientId)?.let {
                        if (it.isNotEmpty()) {
                            cheerEmotes.postValue(it)
                            reloadMessages.value = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load cheermotes for channel $channelId", e)
                }
            }
        }
    }

    fun loadRecentMessages(channelLogin: String, recentMsgLimit: String?) {
        viewModelScope.launch {
            try {
                playerRepository.loadRecentMessages(channelLogin, (recentMsgLimit ?: "100")).body()?.messages?.let {
                    if (it.isNotEmpty()) {
                        recentMessages.postValue(it)
                        reloadMessages.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent messages for channel $channelLogin", e)
            }
        }
    }

    fun reloadEmotes(helixClientId: String?, helixToken: String?, gqlClientId: String?, channelId: String?) {
        savedGlobalBadges = null
        globalStvEmotes = null
        globalBttvEmotes = null
        globalFfzEmotes = null
        loadEmotes(helixClientId, helixToken, gqlClientId, channelId)
    }

    inner class LiveChatController(
            private val useSSl: Boolean,
            private val usePubSub: Boolean,
            private val user: User,
            private val isLoggedIn: Boolean,
            private val helixClientId: String?,
            private val gqlClientId: String?,
            private val channelId: String?,
            private val channelLogin: String,
            displayName: String?,
            private val showUserNotice: Boolean,
            private val showClearMsg: Boolean,
            private val showClearChat: Boolean,
            private val collectPoints: Boolean,
            private val notifyPoints: Boolean) : ChatController(), OnUserStateReceivedListener, OnRoomStateReceivedListener, OnCommandReceivedListener, OnRewardReceivedListener, OnPointsEarnedListener, OnClaimPointsListener, OnMinuteWatchedListener, OnRaidListener, ChatView.RaidCallback, OnViewerCountReceivedListener {

        private var chat: LiveChatThread? = null
        private var loggedInChat: LoggedInChatThread? = null
        private var pubSub: PubSubWebSocket? = null
        private val allEmotesMap = mutableMapOf<String, Emote>()
        private var usedRaidId: String? = null

        val chatters = ConcurrentHashMap<String?, Chatter>()

        init {
            displayName?.let { chatters[it] = Chatter(it) }
        }

        override fun send(message: CharSequence) {
            if (message.toString() == "/dc" || message.toString() == "/disconnect") {
                if (chat?.isActive == true) {
                    disconnect()
                }
            } else {
                loggedInChat?.send(message)
                val usedEmotes = hashSetOf<RecentEmote>()
                val currentTime = System.currentTimeMillis()
                message.split(' ').forEach { word ->
                    allEmotesMap[word]?.let { usedEmotes.add(RecentEmote(word, it.url, currentTime)) }
                }
                if (usedEmotes.isNotEmpty()) {
                    playerRepository.insertRecentEmotes(usedEmotes)
                }
            }
        }

        override fun start() {
            pause()
            chat = TwitchApiHelper.startChat(useSSl, isLoggedIn, channelLogin, showUserNotice, showClearMsg, showClearChat, usePubSub, this, this, this, this, this)
            if (isLoggedIn) {
                loggedInChat = TwitchApiHelper.startLoggedInChat(useSSl, user.login, user.gqlToken?.nullIfEmpty() ?: user.helixToken, channelLogin, showUserNotice, showClearMsg, showClearChat, usePubSub, this, this, this, this, this)
            }
            if (usePubSub && !channelId.isNullOrBlank()) {
                pubSub = TwitchApiHelper.startPubSub(channelId, user.id, user.gqlToken, collectPoints, notifyPoints, showRaids, viewModelScope, this, this, this, this, this, this, this)
            }
        }

        override fun pause() {
            chat?.disconnect()
            loggedInChat?.disconnect()
            pubSub?.disconnect()
        }

        override fun stop() {
            pause()
        }

        override fun onMessage(message: ChatMessage) {
            super.onMessage(message)
            if (message.userName != null && !chatters.containsKey(message.userName)) {
                val chatter = Chatter(message.userName)
                chatters[message.userName] = chatter
                _newChatter.postValue(chatter)
            }
        }

        override fun onUserState(sets: List<String>?) {
            if (savedEmoteSets != sets) {
                viewModelScope.launch {
                    val emotes = mutableListOf<TwitchEmote>()
                    try {
                        if (!helixClientId.isNullOrBlank() && !user.helixToken.isNullOrBlank()) {
                            sets?.asReversed()?.chunked(25)?.forEach { list ->
                                repository.loadEmotesFromSet(helixClientId, user.helixToken, list)?.let { emotes.addAll(it) }
                            }
                        } else if (!gqlClientId.isNullOrBlank() && !user.gqlToken.isNullOrBlank() && !user.id.isNullOrBlank()) {
                            repository.loadUserEmotes(gqlClientId, user.gqlToken, user.id, channelId)?.let { emotes.addAll(it) }
                        }
                    } catch (e: Exception) {
                    }
                    if (emotes.isNotEmpty()) {
                        savedEmoteSets = sets
                        savedUserEmotes = emotes
                        emoteSetsAdded = true
                        val items = emotes.filter { it.ownerId == channelId }
                        for (item in items.asReversed()) {
                            emotes.add(0, item)
                        }
                        addEmotes(emotes)
                        userEmotes.value = emotes
                    }
                }
            } else {
                if (!emoteSetsAdded) {
                    val emotes = mutableListOf<TwitchEmote>()
                    savedUserEmotes?.let { emotes.addAll(it) }
                    if (emotes.isNotEmpty()) {
                        emoteSetsAdded = true
                        val items = emotes.filter { it.ownerId == channelId }
                        for (item in items.asReversed()) {
                            emotes.add(0, item)
                        }
                        addEmotes(emotes)
                        viewModelScope.launch {
                            try {
                                userEmotes.value = emotes
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }
        }

        override fun onRoomState(list: RoomState) {
            roomState.postValue(list)
        }

        override fun onCommand(list: Command) {
            command.postValue(list)
        }

        override fun onReward(message: ChatMessage) {
            reward.postValue(message)
        }

        override fun onPointsEarned(message: PointsEarned) {
            pointsEarned.postValue(message)
        }

        override fun onClaim(message: Claim) {
            viewModelScope.launch {
                repository.loadClaimPoints(gqlClientId, user.gqlToken, message.channelId, message.claimId)
            }
        }

        override fun onMinuteWatched() {
            if (!stream_id.isNullOrBlank()) {
                viewModelScope.launch {
                    repository.loadMinuteWatched(user.id, stream_id, channelId, channelLogin)
                }
            }
        }

        override fun onRaidUpdate(message: Raid) {
            raidNewId = message.raidId != usedRaidId
            raid.postValue(message)
            if (raidNewId) {
                usedRaidId = message.raidId
                if (collectPoints && !user.gqlToken.isNullOrBlank()) {
                    viewModelScope.launch {
                        repository.loadJoinRaid(gqlClientId, user.gqlToken, message.raidId)
                    }
                }
            }
        }

        override fun onRaidClicked() {
            raidAutoSwitch = false
            raidClicked.postValue(true)
        }

        override fun onRaidClose() {
            raidAutoSwitch = false
            raidClosed = true
        }

        override fun onHostClicked() {
            hostClicked.postValue(true)
        }

        override fun onCheckHost() {
            viewModelScope.launch {
                repository.loadHosting(gqlClientId, channelId, channelLogin)?.let {
                    host.postValue(it)
                }
            }
        }

        override fun onViewerCount(viewers: Int?) {
            viewerCount.postValue(viewers)
        }

        fun addEmotes(list: List<Emote>) {
            allEmotesMap.putAll(list.associateBy { it.name })
        }

        fun isActive(): Boolean? {
            return chat?.isActive
        }

        fun disconnect() {
            if (chat?.isActive == true) {
                chat?.disconnect()
                loggedInChat?.disconnect()
                pubSub?.disconnect()
                usedRaidId = null
                command.postValue(Command(type = "disconnect_command"))
            }
        }
    }

    private inner class VideoChatController(
            private val clientId: String?,
            private val videoId: String,
            private val startTime: Double,
            private val getCurrentPosition: () -> Double) : ChatController() {

        private var chatReplayManager: ChatReplayManager? = null

        override fun send(message: CharSequence) {

        }

        override fun start() {
            stop()
            chatReplayManager = ChatReplayManager(clientId, repository, videoId, startTime, getCurrentPosition, this, { _chatMessages.postValue(ArrayList()) }, viewModelScope)
        }

        override fun pause() {
            chatReplayManager?.stop()
        }

        override fun stop() {
            chatReplayManager?.stop()
        }
    }

    abstract inner class ChatController : OnChatMessageReceivedListener {
        abstract fun send(message: CharSequence)
        abstract fun start()
        abstract fun pause()
        abstract fun stop()

        override fun onMessage(message: ChatMessage) {
            _chatMessages.value!!.add(message)
            _newMessage.postValue(message)
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"

        private var savedEmoteSets: List<String>? = null
        private var savedUserEmotes: List<TwitchEmote>? = null
        private var savedGlobalBadges: List<TwitchBadge>? = null
        private var globalStvEmotes: List<StvEmote>? = null
        private var globalBttvEmotes: List<BttvEmote>? = null
        private var globalFfzEmotes: List<FfzEmote>? = null
    }
}