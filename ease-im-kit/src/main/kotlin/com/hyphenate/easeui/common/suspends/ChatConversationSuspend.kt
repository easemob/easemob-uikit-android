package com.hyphenate.easeui.common.suspends

import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.impl.CallbackImpl
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