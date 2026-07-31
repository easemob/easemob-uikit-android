package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatPushRemindType
import com.hyphenate.easeui.model.ChatUIKitConversation

/**
 * Convert [ChatConversation] to [ChatUIKitConversation].
 */
fun ChatConversation.parse() = ChatUIKitConversation(
    conversationId = conversationId(),
    conversationType = type,
    unreadMsgCount = unreadMsgCount,
    lastMessage = lastMessage,
    timestamp = lastMessage?.msgTime ?: 0,
    isPinned = isPinned,
    pinnedTime = pinnedTime
)

/**
 * Whether the conversation is group chat.
 */
val ChatConversation.isGroupChat:Boolean
    get() = type == ChatConversationType.GroupChat

/**
 * Whether the conversation is chat room.
 */
val ChatConversation.isChatroom:Boolean
    get() = type == ChatConversationType.ChatRoom

internal fun resolveConversationSilent(
    pushRemindType: ChatPushRemindType?,
    legacyMuted: Boolean,
): Boolean = pushRemindType?.let { it != ChatPushRemindType.ALL } ?: legacyMuted

internal fun ChatConversation?.isSilentWithLegacyFallback(conversationId: String): Boolean =
    resolveConversationSilent(
        pushRemindType = this?.pushRemindType(),
        legacyMuted = ChatUIKitClient.getCache().getMutedConversationList().containsKey(conversationId),
    )
