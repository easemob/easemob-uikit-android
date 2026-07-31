package com.hyphenate.easeui.feature.invitation.helper

import android.text.TextUtils
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatCallback
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.ChatUIKitConstant
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.model.ChatUIKitEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID


class ChatUIKitNotificationMsgManager {

    private data class RequestReadCursor(
        val version: Int = REQUEST_CURSOR_VERSION,
        val initialized: Boolean = true,
        val readThroughTime: Long = 0L,
        val lastReadMessageId: String? = null,
        val updatedAt: Long = System.currentTimeMillis(),
    )

    private val cursorLock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cursorOwner: String? = null
    private var cachedCursor: RequestReadCursor? = null
    private var lastIssuedRequestTime = 0L

    companion object {

        private const val TAG = "ChatUIKitNotificationMsgManager"
        private const val REQUEST_CURSOR_EXT_KEY = "ease_contact_request_cursor"
        private const val REQUEST_CURSOR_VERSION = 1
        private const val DEFAULT_UNREAD_PAGE_SIZE = 50
        private const val DEFAULT_UNREAD_MAX_COUNT = 100

        private var instance: ChatUIKitNotificationMsgManager? = null
        fun getInstance(): ChatUIKitNotificationMsgManager {
            if (instance == null) {
                synchronized(ChatUIKitNotificationMsgManager::class.java) {
                    if (instance == null) {
                        instance = ChatUIKitNotificationMsgManager()
                    }
                }
            }
            return instance!!
        }
    }

    fun createMessage(message: String?, ext: Map<String, Any>): ChatMessage {
        synchronized(cursorLock) {
            prepareCurrentUserLocked()
            val conversation = getConversation()
            // 必须先建立迁移基线再保存新申请，否则首次升级后的第一条申请会被误当成历史消息。
            val cursor = ensureCursorInitializedLocked(conversation)
            val requestTime = nextRequestTimeLocked(conversation, cursor)
            val emMessage: ChatMessage = ChatMessage.createReceiveMessage(ChatMessageType.TXT)
            emMessage.from = ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID
            emMessage.msgId = UUID.randomUUID().toString()
            emMessage.setStatus(ChatMessageStatus.SUCCESS)
            emMessage.setLocalTime(requestTime)
            emMessage.setMsgTime(requestTime)
            emMessage.addBody(ChatTextMessageBody(message))
            if (ext.isNotEmpty()) {
                val iterator = ext.keys.iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = ext[key]
                    value?.let {
                        putObject(emMessage, key, it)
                    }
                }
            }
            ChatClient.getInstance().chatManager().saveMessage(emMessage)
            return emMessage
        }
    }

    private fun putObject(message: ChatMessage, key: String, value: Any) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        when (value) {
            is String -> {
                message.setAttribute(key, value)
            }

            is Byte -> {
                message.setAttribute(key, (value as Int))
            }

            is Char -> {
                message.setAttribute(key, (value as Int))
            }

            is Short -> {
                message.setAttribute(key, (value as Int))
            }

            is Int -> {
                message.setAttribute(key, value)
            }

            is Boolean -> {
                message.setAttribute(key, value)
            }

            is Long -> {
                message.setAttribute(key, value)
            }

            is Float -> {
                val `object` = JSONObject()
                try {
                    `object`.put(key, value)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                message.setAttribute(key, `object`)
            }

            is Double -> {
                val `object` = JSONObject()
                try {
                    `object`.put(key, value)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                message.setAttribute(key, `object`)
            }

            is JSONObject -> {
                message.setAttribute(key, value)
            }

            is JSONArray -> {
                message.setAttribute(key, value)
            }

            else -> {
                val `object` = JSONObject()
                try {
                    `object`.put(key, value)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                message.setAttribute(key, `object`)
            }
        }
    }

    fun createMsgExt(): MutableMap<String, Any> {
        return HashMap()
    }

    /**
     * Get latest message
     * @param con
     * @return
     */
    fun getLastMessageByConversation(con: ChatConversation?): ChatMessage? {
        return con?.lastMessage
    }


    /**
     * Get notification conversation
     * @return
     */
    fun getConversation():ChatConversation{
        return getSystemConversation(true)
    }

    /**
     * Get notification conversation
     * @param createIfNotExists
     * @return
     */
    fun getSystemConversation(createIfNotExists:Boolean):ChatConversation{
        return ChatClient.getInstance().chatManager().getConversation(
            ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID,
            ChatConversationType.Chat,
            createIfNotExists
        )
    }

    /**
     * Get all messages of notification
     * @return
     */
    fun getAllNotifyMessage():List<ChatMessage>{
        return getConversation().allMessages
    }

    /**
     * load more message of notification
     * @return
     */
    fun loadMoreMessage(startMsgId:String?="",limit:Int):List<ChatMessage>{
        return getConversation().loadMoreMsgFromDB(startMsgId,limit)
    }


    /**
     * Check whether is a notification message
     * @param message
     * @return
     */
    fun isNotificationMessage(message: ChatMessage): Boolean {
        return (message.type === ChatMessageType.TXT
                && TextUtils.equals(message.from, ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID))
    }


    /**
     * Check whether is a notification conversation
     * @param conversation
     * @return
     */
    fun isNotificationConversation(conversation: ChatConversation): Boolean {
        return (conversation.type === ChatConversationType.Chat
            && TextUtils.equals(conversation.conversationId(), ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID)
        )
    }


    /**
     * Get the message content
     * @param message
     * @return
     */
    fun getMessageContent(message: ChatMessage): String? {
        return if (message.body is ChatTextMessageBody) {
            (message.body as ChatTextMessageBody).message
        } else ""
    }

    /**
     * Update notification message
     * @param message
     * @return
     */
    fun updateMessage(message: ChatMessage?): Boolean {
        if (message == null || !isNotificationMessage(message)) {
            return false
        }
        ChatClient.getInstance().chatManager().updateMessage(message)
        return true
    }

    /**
     * Remove notification message
     * @param message
     * @return
     */
    fun removeMessage(message: ChatMessage?): Boolean {
        if (message == null || !isNotificationMessage(message)) {
            return false
        }
        val conversation: ChatConversation = ChatClient.getInstance().chatManager()
            .getConversation(ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID)
        conversation.removeMessage(message.msgId)
        return true
    }

    /**
     * 当前用户数据库打开后初始化本地已读游标。
     * 首次升级时以已有的最新申请为基线，避免把全部历史申请突然展示为未读。
     */
    fun initializeRequestReadCursor() {
        ioScope.launch {
            synchronized(cursorLock) {
                // 同一账号重新打开数据库时也必须丢弃进程缓存，以数据库持久化值为准。
                cachedCursor = null
                lastIssuedRequestTime = 0L
                prepareCurrentUserLocked()
                ensureCursorInitializedLocked(getConversation())
            }
        }
    }

    /**
     * 统计会话游标之后创建的联系人申请。
     * UI 超过 99 只展示 99+，所以计数达到 100 后立即停止继续扫描历史消息。
     */
    suspend fun getRequestUnreadCount(maxCount: Int = DEFAULT_UNREAD_MAX_COUNT): Int {
        if (maxCount <= 0) return 0
        return withContext(Dispatchers.IO) {
            synchronized(cursorLock) {
                prepareCurrentUserLocked()
                val conversation = getConversation()
                val cursor = ensureCursorInitializedLocked(conversation)
                countUnreadRequestsLocked(conversation, cursor, maxCount)
            }
        }
    }

    /**
     * 将当前最新申请所在的时间点写入会话游标，表示该时间及以前的申请全部已读。
     * 游标使用时间而不是消息 ID，因为同意申请后对应消息会被删除。
     */
    fun markAllMessagesAsRead() {
        ioScope.launch {
            synchronized(cursorLock) {
                prepareCurrentUserLocked()
                val conversation = getConversation()
                val cursor = ensureCursorInitializedLocked(conversation)
                val latestRequest = findLatestRequestMessageLocked(conversation)
                val readThroughTime = maxOf(
                    cursor.readThroughTime,
                    latestRequest?.localTime() ?: 0L,
                )
                saveCursorLocked(
                    conversation,
                    cursor.copy(
                        readThroughTime = readThroughTime,
                        lastReadMessageId = latestRequest?.msgId ?: cursor.lastReadMessageId,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
            notifyUnreadCountChanged()
        }

        // 兼容清理旧 SDK/UIKit 遗留的原生会话未读数；新的申请角标不再依赖该值。
        ChatClient.getInstance().chatManager()
            .asyncClearConversationUnreadMessageCount(
                ChatUIKitConstant.DEFAULT_SYSTEM_MESSAGE_ID,
                object : ChatCallback {
                    override fun onSuccess() = Unit

                    override fun onError(code: Int, error: String?) {
                        ChatLog.e(TAG, "Clear legacy notification unread count failed: $code, $error")
                    }
                }
            )
    }

    private fun prepareCurrentUserLocked() {
        val currentUser = ChatClient.getInstance().currentUser
        if (cursorOwner != currentUser) {
            cursorOwner = currentUser
            cachedCursor = null
            lastIssuedRequestTime = 0L
        }
    }

    private fun nextRequestTimeLocked(
        conversation: ChatConversation,
        cursor: RequestReadCursor,
    ): Long {
        val now = System.currentTimeMillis()
        val latestMessageTime = conversation.lastMessage?.localTime() ?: 0L
        // 同时抵御同毫秒多回调和系统时钟回拨，保证新申请一定排在已读游标之后。
        return maxOf(
            now,
            latestMessageTime + 1,
            cursor.readThroughTime + 1,
            lastIssuedRequestTime + 1,
        ).also {
            lastIssuedRequestTime = it
        }
    }

    private fun ensureCursorInitializedLocked(conversation: ChatConversation): RequestReadCursor {
        cachedCursor?.let { return it }
        readCursorLocked(conversation)?.let { cursor ->
            if (cursor.initialized && cursor.version == REQUEST_CURSOR_VERSION) {
                cachedCursor = cursor
                return cursor
            }
        }

        // 没有游标代表首次启用该方案：已有消息全部作为迁移前历史数据处理。
        val latestRequest = findLatestRequestMessageLocked(conversation)
        val cursor = RequestReadCursor(
            readThroughTime = latestRequest?.localTime() ?: 0L,
            lastReadMessageId = latestRequest?.msgId,
        )
        saveCursorLocked(conversation, cursor)
        return cursor
    }

    private fun readCursorLocked(conversation: ChatConversation): RequestReadCursor? {
        val extField = conversation.extField
        if (extField.isNullOrBlank()) return null
        return try {
            val cursorJson = JSONObject(extField).optJSONObject(REQUEST_CURSOR_EXT_KEY) ?: return null
            RequestReadCursor(
                version = cursorJson.optInt("version", 0),
                initialized = cursorJson.optBoolean("initialized", false),
                readThroughTime = cursorJson.optLong("readThroughTime", 0L),
                lastReadMessageId = cursorJson.optString("lastReadMessageId")
                    .takeIf { it.isNotEmpty() },
                updatedAt = cursorJson.optLong("updatedAt", 0L),
            )
        } catch (e: JSONException) {
            ChatLog.e(TAG, "Parse request read cursor failed: ${e.message}")
            null
        }
    }

    private fun saveCursorLocked(conversation: ChatConversation, cursor: RequestReadCursor) {
        // extField 是整段字符串，合并 namespaced JSON 节点以免覆盖后续增加的其他会话状态。
        val root = try {
            conversation.extField?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
        } catch (e: JSONException) {
            ChatLog.e(TAG, "Reset invalid system conversation ext field: ${e.message}")
            JSONObject()
        }
        val cursorJson = JSONObject().apply {
            put("version", cursor.version)
            put("initialized", cursor.initialized)
            put("readThroughTime", cursor.readThroughTime)
            cursor.lastReadMessageId?.let { put("lastReadMessageId", it) }
            put("updatedAt", cursor.updatedAt)
        }
        root.put(REQUEST_CURSOR_EXT_KEY, cursorJson)
        conversation.extField = root.toString()
        // SDK 高层 setExtField 不返回落库结果；保留内存值可保证当前进程内读写立即一致。
        cachedCursor = cursor
    }

    private fun countUnreadRequestsLocked(
        conversation: ChatConversation,
        cursor: RequestReadCursor,
        maxCount: Int,
    ): Int {
        var unreadCount = 0
        var startMsgId = ""
        while (unreadCount < maxCount) {
            val messages = conversation.loadMoreMsgFromDB(startMsgId, DEFAULT_UNREAD_PAGE_SIZE)
            if (messages.isEmpty()) break
            for (message in messages) {
                // SDK 按时间倒序分页，遇到游标即代表后续页面也都是已读历史。
                if (message.localTime() <= cursor.readThroughTime) {
                    return unreadCount
                }
                if (isContactRequestMessage(message)) {
                    unreadCount++
                    if (unreadCount >= maxCount) return maxCount
                }
            }
            startMsgId = messages.last().msgId
        }
        return unreadCount
    }

    private fun findLatestRequestMessageLocked(conversation: ChatConversation): ChatMessage? {
        conversation.lastMessage?.takeIf(::isContactRequestMessage)?.let { return it }
        var startMsgId = ""
        while (true) {
            val messages = conversation.loadMoreMsgFromDB(startMsgId, DEFAULT_UNREAD_PAGE_SIZE)
            if (messages.isEmpty()) return null
            messages.firstOrNull(::isContactRequestMessage)?.let { return it }
            startMsgId = messages.last().msgId
        }
    }

    private fun isContactRequestMessage(message: ChatMessage): Boolean {
        return isNotificationMessage(message) &&
            message.ext().containsKey(ChatUIKitConstant.SYSTEM_MESSAGE_FROM)
    }

    private fun notifyUnreadCountChanged() {
        val context = ChatUIKitClient.getContext()
        context?.let {
            it.mainScope().launch {
                ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name).post(this,
                    ChatUIKitEvent(ChatUIKitEvent.EVENT.UPDATE.name, ChatUIKitEvent.TYPE.NOTIFY)
                )
            }
        }
    }
}
