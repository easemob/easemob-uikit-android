package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.feature.chat.enums.ChatUIKitType
import com.hyphenate.easeui.feature.chat.enums.getConversationType
import com.hyphenate.easeui.model.ChatUIKitConversation
import com.hyphenate.easeui.model.ChatUIKitGroupProfile
import com.hyphenate.easeui.model.chatConversation
import com.hyphenate.easeui.provider.getSyncUser

/** Resolved name and avatar used by every conversation UI entry point. */
internal data class ChatUIKitConversationDisplayInfo(
    val name: String,
    val avatar: String?,
)

internal fun ChatUIKitConversation.getDisplayInfo(): ChatUIKitConversationDisplayInfo =
    resolveConversationDisplayInfo(conversationId, conversationType, chatConversation())

internal fun ChatConversation.getDisplayInfo(): ChatUIKitConversationDisplayInfo =
    resolveConversationDisplayInfo(conversationId(), type, this)

internal fun resolveConversationDisplayInfo(
    conversationId: String?,
    chatType: ChatUIKitType,
): ChatUIKitConversationDisplayInfo = resolveConversationDisplayInfo(
    conversationId = conversationId,
    conversationType = chatType.getConversationType(),
)

internal fun resolveConversationDisplayInfo(
    conversationId: String?,
    conversationType: ChatConversationType,
    conversation: ChatConversation? = conversationId
        ?.takeIf(String::isNotEmpty)
        ?.let { ChatClient.getInstance().chatManager().getConversation(it, conversationType) },
): ChatUIKitConversationDisplayInfo {
    val id = conversationId.orEmpty()
    return when (conversationType) {
        ChatConversationType.Chat -> {
            val providerProfile = ChatUIKitClient.getUserProvider()?.getSyncUser(id)
            resolveSingleDisplayInfo(
                conversationId = id,
                providerRemark = providerProfile?.remark,
                providerName = providerProfile?.name,
                providerAvatar = providerProfile?.avatar,
                sdkName = conversation?.getConversationName(),
                sdkAvatar = conversation?.getConversationAvatar(),
                legacyName = conversation?.latestMessageFromOthers?.getUserInfo()?.name,
            )
        }

        ChatConversationType.GroupChat -> {
            val providerProfile = getCustomGroupProfile(id)
            val localGroup = ChatClient.getInstance().groupManager().getGroup(id)
            resolveGroupDisplayInfo(
                conversationId = id,
                providerName = providerProfile?.name,
                providerAvatar = providerProfile?.avatar,
                sdkName = conversation?.getConversationName(),
                sdkAvatar = conversation?.getConversationAvatar(),
                localName = localGroup?.groupName,
                localAvatar = localGroup?.groupAvatar,
            )
        }

        ChatConversationType.ChatRoom -> {
            val chatRoom = ChatClient.getInstance().chatroomManager().getChatRoom(id)
            resolveChatRoomDisplayInfo(id, chatRoom?.name)
        }
    }
}

private fun getCustomGroupProfile(groupId: String): ChatUIKitGroupProfile? {
    ChatUIKitClient.getCache().getGroup(groupId)?.let { return it }
    return ChatUIKitClient.getGroupProfileProvider()?.getGroup(groupId)?.also {
        ChatUIKitClient.getCache().insertGroup(groupId, it)
    }
}

internal fun resolveSingleDisplayInfo(
    conversationId: String,
    providerRemark: String?,
    providerName: String?,
    providerAvatar: String?,
    sdkName: String?,
    sdkAvatar: String?,
    legacyName: String?,
): ChatUIKitConversationDisplayInfo = ChatUIKitConversationDisplayInfo(
    name = firstNotBlank(providerRemark, providerName, sdkName, legacyName, conversationId).orEmpty(),
    avatar = firstNotBlank(providerAvatar, sdkAvatar),
)

internal fun resolveGroupDisplayInfo(
    conversationId: String,
    providerName: String?,
    providerAvatar: String?,
    sdkName: String?,
    sdkAvatar: String?,
    localName: String?,
    localAvatar: String?,
): ChatUIKitConversationDisplayInfo = ChatUIKitConversationDisplayInfo(
    name = firstNotBlank(providerName, sdkName, localName, conversationId).orEmpty(),
    avatar = firstNotBlank(providerAvatar, sdkAvatar, localAvatar),
)

internal fun resolveChatRoomDisplayInfo(
    conversationId: String,
    localName: String?,
): ChatUIKitConversationDisplayInfo = ChatUIKitConversationDisplayInfo(
    name = firstNotBlank(localName, conversationId).orEmpty(),
    avatar = null,
)

internal fun firstNotBlank(vararg values: String?): String? =
    values.firstOrNull { it.isNullOrBlank().not() }
