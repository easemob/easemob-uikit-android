package com.hyphenate.easeui.repository

import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatManager
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageBody
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.ChatroomManager
import com.hyphenate.easeui.common.extensions.get
import com.hyphenate.easeui.common.suspends.ackConversationToRead
import com.hyphenate.easeui.common.suspends.ackGroupMessageToRead
import com.hyphenate.easeui.common.suspends.ackMessageToRead
import com.hyphenate.easeui.common.suspends.addMessageReaction
import com.hyphenate.easeui.common.suspends.fetchHistoryMessages
import com.hyphenate.easeui.common.suspends.joinChatroom
import com.hyphenate.easeui.common.suspends.leaveChatroom
import com.hyphenate.easeui.common.suspends.modifyMessage
import com.hyphenate.easeui.common.suspends.recallChatMessage
import com.hyphenate.easeui.common.suspends.removeMessageReaction
import com.hyphenate.easeui.common.suspends.reportChatMessage
import com.hyphenate.easeui.common.utils.isMessageIdValid
import com.hyphenate.easeui.model.EaseMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EaseChatManagerRepository(
    private val chatManager: ChatManager = ChatClient.getInstance().chatManager(),
    private val chatroomManager: ChatroomManager = ChatClient.getInstance().chatroomManager()
) {

    suspend fun joinChatroom(roomId: String) =
        withContext(Dispatchers.IO) {
            chatroomManager.joinChatroom(roomId)
        }

    suspend fun leaveChatroom(roomId: String) =
        withContext(Dispatchers.IO) {
            chatroomManager.leaveChatroom(roomId)
        }

    suspend fun loadLocalMessages(conversation: ChatConversation?, startMsgId: String?
                                      , pageSize: Int, direction: ChatSearchDirection): List<EaseMessage> {
        return withContext(Dispatchers.IO) {
            if (conversation == null) {
                throw ChatException(ChatError.INVALID_PARAM, "Should first set up with conversation.")
            }
            if (!isMessageIdValid(startMsgId)) {
                throw ChatException(ChatError.MESSAGE_INVALID, "Invalid message id.")
            }
            conversation.loadMoreMsgFromDB(startMsgId, pageSize, direction).map {
                if (it.status() == ChatMessageStatus.CREATE) {
                    it.setStatus(ChatMessageStatus.FAIL)
                }
                it.get()
            }
        }
    }

    suspend fun fetchRoamMessages(conversation: ChatConversation?, startMsgId: String?
                                      , pageSize: Int, direction: ChatSearchDirection) =
        withContext(Dispatchers.IO) {
            if (conversation == null) {
                throw ChatException(ChatError.INVALID_PARAM, "Should first set up with conversation.")
            }
            if (!isMessageIdValid(startMsgId)) {
                throw ChatException(ChatError.MESSAGE_INVALID, "Invalid message id.")
            }
            chatManager.fetchHistoryMessages(conversation.conversationId(), conversation.type
                , startMsgId, pageSize, direction).data.map { it.get() }
        }

    suspend fun reportMessage(tag:String,reason:String?="",msgId: String):Int =
        withContext(Dispatchers.IO) {
            chatManager.reportChatMessage(msgId,tag,reason)
        }

    suspend fun recallMessage(message: ChatMessage?) =
        withContext(Dispatchers.IO) {
            chatManager.recallChatMessage(message)
        }

    suspend fun modifyMessage(messageId: String?, messageBodyModified: ChatMessageBody?) =
        withContext(Dispatchers.IO) {
            chatManager.modifyMessage(messageId, messageBodyModified)
        }

    suspend fun addReaction(message: ChatMessage?, reaction: String?) =
        withContext(Dispatchers.IO) {
            chatManager.addMessageReaction(message, reaction)
        }

    suspend fun removeReaction(message: ChatMessage?, reaction: String?) =
        withContext(Dispatchers.IO) {
            chatManager.removeMessageReaction(message, reaction)
        }

    suspend fun ackConversationRead(conversationId: String?) =
        withContext(Dispatchers.IO) {
            chatManager.ackConversationToRead(conversationId)
        }

    suspend fun ackGroupMessageRead(conversationId: String?, messageId: String?, ext: String?) =
        withContext(Dispatchers.IO) {
            chatManager.ackGroupMessageToRead(conversationId, messageId, ext)
        }

    suspend fun ackMessageRead(conversationId: String?, messageId: String?) =
        withContext(Dispatchers.IO) {
            chatManager.ackMessageToRead(conversationId, messageId)
        }

}