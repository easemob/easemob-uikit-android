package com.hyphenate.easeui.viewmodel.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.get
import com.hyphenate.easeui.common.suspends.deleteMessage
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageListResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.repository.EaseChatManagerRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

open class EaseMessageListViewModel(
    override var pageSize: Int = 10
): ViewModel(), IChatMessageListRequest {

    private var _view: IChatMessageListResultView? = null
    private var _conversation: ChatConversation? = null
    private val chatRepository by lazy { EaseChatManagerRepository() }

    override fun attachView(view: IControlDataView) {
        _view = view as IChatMessageListResultView
    }

    override var messageCursor: String? = null

    override fun setupWithConversation(conversation: ChatConversation?) {
        _conversation = conversation
        _conversation?.let {
            // Chat thread conversation should clear cache data.
            if (it.isChatThread) it.clear()
        }
    }

    override fun joinChatroom(roomId: String) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.joinChatroom(roomId))
            }
            .catchChatException { e ->
                _view?.joinChatRoomFail(e.errorCode, e.description)
            }
            .collect {
                _view?.joinChatRoomSuccess(it)
            }
        }
    }

    override fun leaveChatroom(roomId: String) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.leaveChatroom(roomId))
            }
            .catchChatException { e ->
                _view?.leaveChatRoomFail(e.errorCode, e.description)
            }
            .collect {
                _view?.leaveChatRoomSuccess()
            }
        }
    }

    override fun getAllCacheMessages() {
        viewModelScope.launch {
            if (_conversation == null) {
                _view?.getAllMessagesFail(ChatError.INVALID_PARAM, "The conversation is null.")
                return@launch
            }
            _conversation?.run {
                // Mark all messages as read. App may be crashed, so mark all messages as read when get all messages.
                markAllMessagesAsRead()
                _view?.getAllMessagesSuccess(allMessages.map { it.get() })
            }
        }
    }

    override fun loadLocalMessages(direction: ChatSearchDirection) {
        viewModelScope.launch {
            messageCursor = ""
            flow {
                emit(chatRepository.loadLocalMessages(_conversation, messageCursor, pageSize, direction))
            }
            .catchChatException { e ->
                _view?.loadLocalMessagesFail(e.errorCode, e.description)
            }
            .collect {
                messageCursor = if (it.firstMessageId()?.isEmpty() == true) messageCursor else it.firstMessageId()
                _view?.loadLocalMessagesSuccess(it)
            }
        }
    }

    override fun loadMoreLocalMessages(
        startMsgId: String?,
        direction: ChatSearchDirection
    ) {
        viewModelScope.launch {
            val startMessageId = if (startMsgId.isNullOrEmpty()) {
                if (messageCursor.isNullOrEmpty()) {
                    _conversation?.allMessages?.getFirstMessageId() ?: messageCursor
                } else messageCursor
            } else startMsgId
            flow {
                emit(chatRepository.loadLocalMessages(_conversation, startMessageId, pageSize, direction))
            }
            .catchChatException { e ->
                _view?.loadMoreLocalMessagesFail(e.errorCode, e.description)
            }
            .collect {
                messageCursor = if (it.firstMessageId()?.isEmpty() == true) messageCursor else it.firstMessageId()
                _view?.loadMoreLocalMessagesSuccess(it)
            }
        }
    }

    override fun fetchRoamMessages(direction: ChatSearchDirection) {
        viewModelScope.launch {
            messageCursor = ""
            flow {
                emit(chatRepository.fetchRoamMessages(_conversation, messageCursor, pageSize, direction))
            }
            .catchChatException { e ->
                _view?.fetchRoamMessagesFail(e.errorCode, e.description)
            }
            .collect {
                messageCursor = if (it.firstMessageId()?.isEmpty() == true) messageCursor else it.firstMessageId()
                _view?.fetchRoamMessagesSuccess(it)
            }
        }
    }

    override fun fetchMoreRoamMessages(
        startMsgId: String?,
        direction: ChatSearchDirection
    ) {
        viewModelScope.launch {
            val startMessageId = if (startMsgId.isNullOrEmpty()) messageCursor else startMsgId
            flow {
                emit(chatRepository.fetchRoamMessages(_conversation, startMessageId, pageSize, direction))
            }
            .catchChatException { e ->
                _view?.fetchMoreRoamMessagesFail(e.errorCode, e.description)
            }
            .collect {
                messageCursor = if (it.firstMessageId()?.isEmpty() == true) messageCursor else it.firstMessageId()
                _view?.fetchMoreRoamMessagesSuccess(it)
            }
        }
    }

    override fun loadLocalHistoryMessages(
        startMsgId: String?,
        direction: ChatSearchDirection
    ) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.loadLocalMessages(_conversation, startMsgId, pageSize, direction))
            }
            .catchChatException { e ->
                _view?.loadLocalHistoryMessagesFail(e.errorCode, e.description)
            }
            .collect {
                _view?.loadLocalHistoryMessagesSuccess(it, direction)
            }
        }
    }

    override fun loadMoreRetrievalsMessages(msgId: String?, pageSize: Int) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.loadLocalMessages(_conversation, msgId, pageSize, ChatSearchDirection.UP))
            }
            .catchChatException {}
            .collect {
                _view?.loadMoreRetrievalsMessagesSuccess(it)
            }
        }
    }

    override fun removeMessage(message: EaseMessage?, isDeleteServerMessage: Boolean) {
        if (message == null) {
            ChatLog.e(TAG, "removeMessage: The message is null.")
            _view?.removeMessageFail(ChatError.MESSAGE_INVALID, "The message is null.")
            return
        }
        if (_conversation == null) {
            ChatLog.e(TAG, "removeMessage: The conversation is null.")
            _view?.removeMessageFail(ChatError.INVALID_PARAM, "The conversation is null.")
            return
        }
        viewModelScope.launch {
            if (isDeleteServerMessage) {
                flow {
                    emit(_conversation?.deleteMessage(mutableListOf(message.getMessage().msgId)))
                }
                .catchChatException { e ->
                    _view?.removeMessageFail(e.errorCode, e.description)
                }
                .collect {
                    _conversation?.removeMessage(message.getMessage().msgId)
                    _view?.removeMessageSuccess(message)
                }
            } else {
                _conversation?.removeMessage(message.getMessage().msgId)
                _view?.removeMessageSuccess(message)
            }
        }
    }

    companion object {
        private val TAG = EaseMessageListViewModel::class.java.simpleName
    }

    private fun List<EaseMessage>.firstMessageId(): String? {
        return if (isEmpty()) "" else first().getMessage().msgId
    }

    private fun List<ChatMessage>.getFirstMessageId(): String? {
        return if (isEmpty()) "" else first().msgId
    }
}