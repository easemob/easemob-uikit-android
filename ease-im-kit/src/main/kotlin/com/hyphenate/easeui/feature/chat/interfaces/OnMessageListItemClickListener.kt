package com.hyphenate.easeui.feature.chat.interfaces

import android.view.View
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.model.EaseReactionEmojiconEntity

/**
 * Item click listener for message list
 */
interface OnMessageListItemClickListener {
    /**
     * Click the bubble.
     * If you want handle it, return true and add you owner logic.
     * @param message
     * @return
     */
    fun onBubbleClick(message: ChatMessage?): Boolean

    /**
     * Long click the bubble.
     * @param v The view of bubble
     * @param message
     */
    fun onBubbleLongClick(v: View?, message: ChatMessage?): Boolean

    /**
     * Click the resend view.
     * @param message
     * @return
     */
    fun onResendClick(message: ChatMessage?): Boolean

    /**
     * Click the user avatar.
     * @param userId
     */
    fun onUserAvatarClick(userId: String?)

    /**
     * Long click the user avatar.
     * @param userId
     */
    fun onUserAvatarLongClick(userId: String?)

    /**
     * Click the chat thread region.
     * @param messageId The chat thread's parent message id.
     * @param threadId  The chat thread id.
     * @return Boolean True if you want handle it.
     */
    fun onChatThreadClick(messageId: String?, threadId: String?): Boolean {
        return false
    }

    /**
     * Long click the chat thread region.
     * @param messageId The chat thread's parent message id.
     * @param threadId  The chat thread id.
     * @return Boolean True if you want handle it.
     */
    fun onChatThreadLongClick(v: View?, messageId: String?, threadId: String?): Boolean {
        return false
    }

    /**
     * Click the remove message reaction view.
     *
     * @param message           The message which the reaction is removed from.
     * @param reactionEntity    The reaction entity which is removed.
     */
    fun onRemoveMessageReaction(message: ChatMessage?, reactionEntity: EaseReactionEmojiconEntity?) {}

    /**
     * Click the add message reaction view.
     *
     * @param message           The message which the reaction is added to.
     * @param reactionEntity    The reaction entity which is added.
     */
    fun onAddMessageReaction(message: ChatMessage?, reactionEntity: EaseReactionEmojiconEntity?) {}
}