package com.hyphenate.easeui.interfaces

import com.hyphenate.chat.EMGroupReadAck
import com.hyphenate.chat.EMMessage
import com.hyphenate.chat.EMMessageReactionChange
import com.hyphenate.easeui.common.ChatMessageListener

open class EaseMessageListener: ChatMessageListener {
    override fun onMessageReceived(messages: MutableList<EMMessage>?) {}

    override fun onCmdMessageReceived(messages: List<EMMessage?>?) {}

    override fun onMessageRead(messages: List<EMMessage?>?) {}

    override fun onGroupMessageRead(groupReadAcks: List<EMGroupReadAck?>?) {}

    override fun onReadAckForGroupMessageUpdated() {}

    override fun onMessageDelivered(messages: List<EMMessage?>?) {}

    override fun onMessageRecalled(messages: List<EMMessage?>?) {}

    override fun onReactionChanged(messageReactionChangeList: List<EMMessageReactionChange?>?) {}

    override fun onMessageContentChanged(
        messageModified: EMMessage?,
        operatorId: String?,
        operationTime: Long
    ) {}
}