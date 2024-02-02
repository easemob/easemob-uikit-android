package com.hyphenate.easeui.model

import com.hyphenate.easeui.common.ChatCallback
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.extensions.isSuccess
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.feature.chat.config.EaseChatMessageItemConfig
import com.hyphenate.easeui.feature.chat.controllers.EaseChatCacheDataController
import com.hyphenate.easeui.common.impl.ChatCallbackWrapper

/**
 * Only use in EaseUIKit.
 */
class EaseMessage private constructor(
    private val message: ChatMessage
) {
    private val callbackWrapper by lazy { ChatCallbackWrapper() }
    private val downloadCallbackWrapper by lazy { ChatCallbackWrapper() }
    private var config: EaseChatMessageItemConfig? = null
    // Whether the message is checked.
    var isChecked: Boolean = false

    init {
        if (!message.isSuccess()) {
            setMessageCallback()
        }
    }

    private fun setMessageCallback() {
        message.setMessageStatusCallback(CallbackImpl(onSuccess = {
            callbackWrapper.onSuccess()
            callbackWrapper.clear()
        }, onError = { code, error ->
            callbackWrapper.onError(code, error)
            callbackWrapper.clear()
        }, onProgress = { progress ->
            callbackWrapper.onProgress(progress, "")
        }))
    }

    /**
     * Set the callback when you want to listener the status change of message.
     */
    fun setMessageStatusCallback(callback: ChatCallback, isSend: Boolean = false) {
        if (isSend) {
            callbackWrapper.addCallback(callback)
            setMessageCallback()
        } else {
            if (!message.isSuccess()) {
                callbackWrapper.addCallback(callback)
            }
        }
    }

    /**
     * Set the callback when you want to listener the download status change of message.
     */
    fun setDownloadStatusCallback(callback: ChatCallback, isMandatorySet: Boolean = false) {
        val hasCallback = downloadCallbackWrapper.hasCallback()
        downloadCallbackWrapper.addCallback(callback)
        if (!hasCallback || isMandatorySet) {
            message.setMessageStatusCallback(CallbackImpl(onSuccess = {
                downloadCallbackWrapper.onSuccess()
                downloadCallbackWrapper.clear()
            }, onError = { code, error ->
                downloadCallbackWrapper.onError(code, error)
                downloadCallbackWrapper.clear()
            }, onProgress = { progress ->
                downloadCallbackWrapper.onProgress(progress, "")
            }))
        }
    }

    fun getMessage(): ChatMessage {
        return message
    }

    fun setConfig(config: EaseChatMessageItemConfig?) {
        this.config = config
    }

    fun getConfig(): EaseChatMessageItemConfig? {
        return config
    }

    companion object {

        /**
         * Create a new EaseMessage.
         */
        fun create(message: ChatMessage, isNew: Boolean = false): EaseMessage {
            if (isNew) {
                val easeMessage = EaseMessage(message)
                EaseChatCacheDataController.addMessage(easeMessage)
                return easeMessage
            }
            return EaseChatCacheDataController.getMessage(message.conversationId(), message.msgId)?.let {
                // Update message body when message is edited.
                it.message.body = message.body
                it
            } ?: kotlin.run {
                val easeMessage = EaseMessage(message)
                EaseChatCacheDataController.addMessage(easeMessage)
                easeMessage
            }
        }
    }
}