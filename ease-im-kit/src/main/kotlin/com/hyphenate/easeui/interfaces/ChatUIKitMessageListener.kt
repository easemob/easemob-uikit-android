package com.hyphenate.easeui.interfaces

import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageListener
import com.hyphenate.easeui.common.ChatMessagePinInfo
import com.hyphenate.easeui.common.ChatMessagePinOperation
import com.hyphenate.easeui.common.ChatMessageReactionChange
import com.hyphenate.easeui.common.ChatMessageReadReceipt
import com.hyphenate.easeui.common.ChatRecallMessageInfo

open class ChatUIKitMessageListener: ChatMessageListener {

    override fun onMessageReceived(messages: MutableList<ChatMessage>?) {}

    override fun onCmdMessageReceived(messages: MutableList<ChatMessage>?) {}

    override fun onMessageReadReceipts(receipts: MutableList<ChatMessageReadReceipt>?) {}

    override fun onMessageDelivered(messages: MutableList<ChatMessage>?) {}

    override fun onMessageRecalledWithExt(recallMessageInfo: MutableList<ChatRecallMessageInfo>?) {}

    override fun onReactionChanged(messageReactionChangeList: MutableList<ChatMessageReactionChange>?) {}

    override fun onMessageContentChanged(
        messageModified: ChatMessage?,
        operatorId: String?,
        operationTime: Long
    ) {}

    override fun onMessagePinChanged(
        messageId: String?,
        conversationId: String?,
        pinOperation: ChatMessagePinOperation?,
        pinInfo: ChatMessagePinInfo?
    ) {
    }
}