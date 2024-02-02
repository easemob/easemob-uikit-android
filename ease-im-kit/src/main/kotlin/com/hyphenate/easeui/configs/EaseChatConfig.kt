package com.hyphenate.easeui.configs

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.extensions.getBooleanResource
import com.hyphenate.easeui.common.extensions.getIntegerResource

class EaseChatConfig {

    /**
     * The config that whether to enable the function of replying message.
     */
    var enableReplyMessage: Boolean = false
        get() {
            if (field) return field
            if (isUIKitInitialized()) {
                return EaseIM.getContext()?.getBooleanResource(R.bool.ease_enable_message_reply) ?: false
            }
            return false
        }

    /**
     * The config that whether to enable the function of combining message.
     */
    var enableSendCombineMessage: Boolean = false
        get() {
            if (field) return field
            if (isUIKitInitialized()) {
                return EaseIM.getContext()?.getBooleanResource(R.bool.ease_enable_message_combine) ?: false
            }
            return false
        }

    /**
     * The config that whether to enable the function of modifying message after sent.
     */
    var enableModifyMessageAfterSent: Boolean = false
        get() {
            if (field) return field
            if (isUIKitInitialized()) {
                return EaseIM.getContext()?.getBooleanResource(R.bool.ease_enable_message_modify) ?: false
            }
            return false
        }

    /**
     * Set the time period within which messages can be recalled, in milliseconds
     */
    var timePeriodCanRecallMessage: Long = -1L
        get() {
            if (field != -1L) return field
            if (isUIKitInitialized()) {
                return EaseIM.getContext()?.getIntegerResource(R.integer.ease_chat_message_recall_period)?.toLong() ?: -1L
            }
            return -1L
        }

    /**
     * Check if the UIKit is initialized.
     */
    private fun isUIKitInitialized(): Boolean {
        return EaseIM.isInited()
    }

    /**
     * The max width of the image that will be shown in the chat page.
     */
    internal var maxShowWidthRadio: Float = 3 / 5f

    /**
     * The max height of the image that will be shown in the chat page.
     */
    internal var maxShowHeightRadio: Float = 3 / 8f

    /**
     * The config that whether to enable the function of sending channel ack.
     */
    internal var enableSendChannelAck: Boolean = true

    internal var intervalDismissInChatList: Long = -1L
        get() {
            if (field != -1L) return field
            if (isUIKitInitialized()) {
                return EaseIM.getContext()?.getIntegerResource(R.integer.ease_chat_item_timestamp_dismiss_interval)?.toLong() ?: -1L
            }
            return -1L
        }
}