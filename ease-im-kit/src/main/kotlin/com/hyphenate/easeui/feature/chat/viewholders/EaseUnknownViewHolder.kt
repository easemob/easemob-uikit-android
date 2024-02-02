package com.hyphenate.easeui.feature.chat.viewholders

import android.view.View
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.helper.EaseDingMessageHelper
import com.hyphenate.easeui.model.EaseMessage

class EaseUnknownViewHolder(itemView: View) : EaseChatRowViewHolder(itemView) {
    override fun handleReceiveMessage(message: EaseMessage?) {
        super.handleReceiveMessage(message)

        EaseIM.getConfig()?.chatConfig?.run {
            if (enableSendChannelAck) {
                //Here no longer send read_ack message separately, instead enter the chat page to send channel_ack
                //New messages are sent in the onReceiveMessage method of the chat page, except for video
                // , voice and file messages, and send read_ack messages
                message?.getMessage()?.run {
                    if (!isAcked && chatType === ChatType.Chat) {
                        try {
                            ChatClient.getInstance().chatManager()
                                .ackMessageRead(from, msgId)
                        } catch (e: ChatException) {
                            e.printStackTrace()
                        }
                        return
                    }
                    // Send the group-ack cmd type msg if this msg is a ding-type msg.
                    EaseDingMessageHelper.get().sendAckMessage(this)
                }
            }
        }
    }
}