package com.hyphenate.easeui.feature.chat.interfaces

import com.hyphenate.easeui.common.ChatMessage

interface OnMessageReadReceiptSendCallback {
    /**
     * Callback after the message read receipt is sent successfully
     * @param message
     */
    fun onSendReadReceiptSuccess(message: ChatMessage?) {}

    /**
     * Callback when sending the message read receipt fails.
     * @param message
     * @param code
     * @param errorMsg
     */
    fun onSendReadReceiptError(message: ChatMessage?, code: Int, errorMsg: String?)
}