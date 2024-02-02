package com.hyphenate.easeui.feature.chat.controllers

import com.hyphenate.easeui.model.EaseMessage

/**
 * Only used for cache data in chat fragment.
 */
internal object EaseChatCacheDataController {

    private val cacheData by lazy { mutableMapOf<String, MutableMap<String, EaseMessage>>() }

    @Synchronized
    fun addMessage(message: EaseMessage) {
        if (cacheData.containsKey(message.getMessage().conversationId())) {
            cacheData[message.getMessage().conversationId()]?.put(message.getMessage().msgId, message)
            return
        }
        cacheData[message.getMessage().conversationId()] = mutableMapOf(Pair(message.getMessage().msgId, message))
    }

    @Synchronized
    fun addMessages(messages: List<EaseMessage>) {
        messages.forEach { addMessage(it) }
    }

    @Synchronized
    fun getMessage(conversationId: String?, msgId: String?): EaseMessage? {
        return cacheData[conversationId]?.get(msgId)
    }

    @Synchronized
    fun removeMessage(conversationId: String?, msgId: String?) {
        cacheData[conversationId]?.remove(msgId)
    }

    @Synchronized
    fun clear(conversationId: String?) {
        cacheData[conversationId]?.clear()
    }

}