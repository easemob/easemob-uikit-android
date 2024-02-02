package com.hyphenate.easeui.common.suspends

import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatCursorResult
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatManager
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageBody
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.common.impl.ValueCallbackImpl
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Suspend method for [ChatManager.fetchConversationsFromServer(Int, String, ValueCallback)]
 *
 * @param limit The number of conversations that you expect to get on each page. The value range is [1,50].
 * @param cursor The position from which to start to get data. If you pass in `null` or an empty string (""),
 *                  the SDK retrieves conversations from the latest active one.
 * @return [ChatCursorResult] The result of the request.
 */
suspend fun ChatManager.fetchConversationsFromServer(limit: Int, cursor: String?): ChatCursorResult<ChatConversation> {
    return suspendCoroutine { continuation ->
        asyncFetchConversationsFromServer(limit, cursor, ValueCallbackImpl<ChatCursorResult<ChatConversation>>(
                onSuccess = { value ->
                    continuation.resume(value)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncFetchPinnedConversationsFromServer(Int, String, ValueCallback)]
 * @param limit The number of conversations that you expect to get on each page. The value range is [1,50].
 * @param cursor The position from which to start to get data. If you pass in `null` or an empty string (""),
 *                 the SDK retrieves the pinned conversations from the latest pinned one.
 * @return [ChatCursorResult] The result of the request.
 */
suspend fun ChatManager.fetchPinedConversationsFromServer(limit: Int, cursor: String?): ChatCursorResult<ChatConversation> {
    return suspendCoroutine { continuation ->
        asyncFetchPinnedConversationsFromServer(limit, cursor, ValueCallbackImpl<ChatCursorResult<ChatConversation>>(
                onSuccess = { value ->
                    continuation.resume(value)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncPinConversation(String, Boolean, Callback)]
 *
 * @param conversationId The id of the conversation to be pinned.
 * @param isPinned Whether to pin or unpin the conversation.
 * @return [ChatError] The result of the request.
 */
suspend fun ChatManager.pinConversation(conversationId: String?, isPinned: Boolean): Int {
    return suspendCoroutine { continuation ->
        asyncPinConversation(conversationId, isPinned, CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.deleteConversationFromServer(String, ChatConversationType, Boolean, Callback)]
 *
 * @param conversationId The id of the conversation to be deleted.
 * @param conversationType The type of the conversation to be deleted.
 * @param isDeleteServerMessages Whether to delete the messages of the conversation on the server.
 * @return [ChatError] The result of the request.
 */
suspend fun ChatManager.deleteConversationFromServer(
    conversationId: String?,
    conversationType: ChatConversationType,
    isDeleteServerMessages: Boolean
): Int {
    return suspendCoroutine { continuation ->
        deleteConversationFromServer(conversationId,
            conversationType,
            isDeleteServerMessages,
            CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncFetchHistoryMessage]
 *
 * @param conversationId
 * @param conversationType
 * @param startMsgId
 * @param pageSize
 * @param direction
 */
suspend fun ChatManager.fetchHistoryMessages(
    conversationId: String?,
    conversationType: ChatConversationType,
    startMsgId: String?,
    pageSize: Int,
    direction: ChatSearchDirection
): ChatCursorResult<ChatMessage> {
    return suspendCoroutine { continuation ->
        asyncFetchHistoryMessage(conversationId,
            conversationType,
            pageSize,
            startMsgId,
            direction,
            ValueCallbackImpl<ChatCursorResult<ChatMessage>>(
                onSuccess = { value ->
                    continuation.resume(value)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncRecallMessage]
 *
 * @param message
 */
suspend fun ChatManager.recallChatMessage(message: ChatMessage?): Int {
    return suspendCoroutine { continuation ->
        asyncRecallMessage(message, CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncModifyMessage]
 *
 * @param messageId
 * @param messageBodyModified
 */
suspend fun ChatManager.modifyMessage(messageId: String?, messageBodyModified: ChatMessageBody?): ChatMessage {
    return suspendCoroutine { continuation ->
        asyncModifyMessage(messageId, messageBodyModified, ValueCallbackImpl<ChatMessage>(
                onSuccess = {
                    continuation.resume(it)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncAddReaction]
 * @param message
 * @param reaction
 */
suspend fun ChatManager.addMessageReaction(message: ChatMessage?, reaction: String?): Int {
    return suspendCoroutine { continuation ->
        asyncAddReaction(message?.msgId, reaction, CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.asyncRemoveReaction]
 */
suspend fun ChatManager.removeMessageReaction(message: ChatMessage?, reaction: String?): Int {
    return suspendCoroutine { continuation ->
        asyncRemoveReaction(message?.msgId, reaction, CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}

/**
 * Suspend method for [ChatManager.ackConversationRead]
 */
suspend fun ChatManager.ackConversationToRead(conversationId: String?): Int {
    return suspendCoroutine { continuation ->
        try {
            ackConversationRead(conversationId)
            continuation.resume(ChatError.EM_NO_ERROR)
        } catch (e: ChatException) {
            continuation.resumeWithException(ChatException(e.errorCode, e.message))
        }
    }
}

/**
 * Suspend method for [ChatManager.ackGroupMessageRead]
 */
suspend fun ChatManager.ackGroupMessageToRead(conversationId: String?, messageId: String?, ext: String?): Int {
    return suspendCoroutine { continuation ->
        try {
            ackGroupMessageRead(conversationId, messageId, ext)
            continuation.resume(ChatError.EM_NO_ERROR)
        } catch (e: ChatException) {
            continuation.resumeWithException(ChatException(e.errorCode, e.message))
        }
    }
}

/**
 * Suspend method for [ChatManager.ackMessageRead]
 */
suspend fun ChatManager.ackMessageToRead(conversationId: String?, messageId: String?): Int {
    return suspendCoroutine { continuation ->
        try {
            ackMessageRead(conversationId, messageId)
            continuation.resume(ChatError.EM_NO_ERROR)
        } catch (e: ChatException) {
            continuation.resumeWithException(ChatException(e.errorCode, e.message))
        }
    }
}

/**
 * Suspend method for [ChatManager.downloadAttachment]
 */
suspend fun ChatManager.downloadAttachmentBySuspend(message: ChatMessage?): Pair<Int, Int> {
    return suspendCoroutine { continuation ->
        if (message == null) {
            continuation.resumeWithException(ChatException(ChatError.MESSAGE_INVALID, "message is null."))
        } else {
            message.setMessageStatusCallback(
                CallbackImpl(
                    onSuccess = {
                        continuation.resume(Pair(ChatError.EM_NO_ERROR, 100))
                    },
                    onError = { code, message ->
                        continuation.resumeWithException(ChatException(code, message))
                    },
                    onProgress = { progress ->
                        continuation.resume(Pair(-1, progress))
                    }
                )
            )
            downloadAttachment(message)
        }
    }
}

/**
 * Suspend method for [ChatManager.downloadThumbnail]
 */
suspend fun ChatManager.downloadThumbnailBySuspend(message: ChatMessage?): Pair<Int, Int> {
    return suspendCoroutine { continuation ->
        if (message == null) {
            continuation.resumeWithException(ChatException(ChatError.MESSAGE_INVALID, "message is null."))
        } else {
            message.setMessageStatusCallback(
                CallbackImpl(
                    onSuccess = {
                        continuation.resume(Pair(ChatError.EM_NO_ERROR, 100))
                    },
                    onError = { code, message ->
                        continuation.resumeWithException(ChatException(code, message))
                    },
                    onProgress = { progress ->
                        continuation.resume(Pair(-1, progress))
                    }
                )
            )
            downloadThumbnail(message)
        }
    }
}

suspend fun ChatManager.reportChatMessage(messageId: String?,tag:String,reason:String?=""):Int{
    return suspendCoroutine { continuation ->
        asyncReportMessage(messageId,tag,reason, CallbackImpl(
                onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                },
                onError = { code, message ->
                    continuation.resumeWithException(ChatException(code, message))
                }
            )
        )
    }
}
