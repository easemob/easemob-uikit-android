package com.hyphenate.easeui.common.suspends

import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.ChatSearchScope
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.common.impl.ValueCallbackImpl
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Suspend method for [ChatConversation.removeMessagesFromServer].
 * @param messages List of messages to be deleted from the server.
 */
suspend fun ChatConversation.deleteMessage(messages: List<String>): Int =
    suspendCoroutine { continuation ->
        removeMessagesFromServer(messages, CallbackImpl(
            onSuccess = {
                continuation.resume(ChatError.EM_NO_ERROR)
            },
            onError = { code, error ->
                continuation.resumeWithException(ChatException(code, error))
            }
        ))
    }

/**
 * Suspend method for [ChatConversation.searchMessage].
 * @param keywords
 * @param timeStamp
 * @param maxCount
 * @param from
 * @param direction
 * @param chatScope
 */
suspend fun ChatConversation.searchMessage(
    keywords:String,
    timeStamp:Long,
    maxCount:Int,
    from:String?,
    direction:ChatSearchDirection,
    chatScope:ChatSearchScope
):List<ChatMessage> =
    suspendCoroutine { continuation ->
        asyncSearchMsgFromDB(
            keywords,
            timeStamp,
            maxCount,
            from?.takeIf { it.isNotEmpty() }?.let(::listOf),
            direction,
            chatScope,
            ValueCallbackImpl(
                onSuccess = { continuation.resume(it) },
                onError = { code, error ->
                    continuation.resumeWithException(ChatException(code, error))
                }
            )
        )
    }
