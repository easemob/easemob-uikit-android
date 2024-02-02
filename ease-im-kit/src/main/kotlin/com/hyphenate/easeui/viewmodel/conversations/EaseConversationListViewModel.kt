package com.hyphenate.easeui.viewmodel.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatManager
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.collectWithCheckErrorCode
import com.hyphenate.easeui.feature.conversation.interfaces.IEaseConvListResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseConversation
import com.hyphenate.easeui.repository.EaseConversationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class EaseConversationListViewModel(
    private val chatManager: ChatManager = ChatClient.getInstance().chatManager(),
    private val stopTimeoutMillis: Long = 5000
): ViewModel(), IConversationListRequest {

    private val repository: EaseConversationRepository = EaseConversationRepository(chatManager)
    private var _view: IEaseConvListResultView? = null
    override fun attachView(view: IControlDataView) {
        _view = view as IEaseConvListResultView
    }

    override fun loadData() {
        viewModelScope.launch {
            flow {
                emit(repository.loadData())
            }
            .catchChatException { e ->
                _view?.loadConversationListFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.loadConversationListSuccess(it)
                }
            }
        }
    }

    override fun sortConversationList(conversations: List<EaseConversation>) {
        // If you need to sort the conversation list, you can override this method.
        // Then call _view?.loadConversationListSuccess(conversations) to update the UI.
    }

    override fun makeConversionRead(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.makeConversionRead(conversation))
            }
            .catchChatException { e ->
                _view?.loadConversationListFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.makeConversionReadSuccess(position, conversation)
            }
        }
    }

    override fun makeSilentForConversation(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.makeSilentForConversation(conversation))
            }
            .catchChatException { e ->
                _view?.makeSilentForConversationFail(conversation, e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    ChatLog.e("conversation", "makeSilentForConversationSuccess")
                    _view?.makeSilentForConversationSuccess(position, conversation)
                }
            }
        }
    }

    override fun cancelSilentForConversation(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.cancelSilentForConversation(conversation))
            }
            .catchChatException { e ->
                _view?.cancelSilentForConversationFail(conversation, e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.cancelSilentForConversationSuccess(position, conversation)
            }
        }
    }

    override fun pinConversation(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.pinConversation(conversation))
            }
            .catchChatException { e ->
                _view?.pinConversationFail(conversation, e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.pinConversationSuccess(position, conversation)
            }
        }
    }

    override fun unpinConversation(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.unpinConversation(conversation))
            }
            .catchChatException { e ->
                _view?.unpinConversationFail(conversation, e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.unpinConversationSuccess(position, conversation)
            }
        }
    }

    override fun deleteConversation(position: Int, conversation: EaseConversation) {
        viewModelScope.launch {
            flow {
                emit(repository.deleteConversation(conversation))
            }
            .catchChatException { e ->
                _view?.deleteConversationFail(conversation, e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.deleteConversationSuccess(position, conversation)
            }
        }
    }

    override fun fetchConvInfo(conversationList: List<EaseConversation>) {
        viewModelScope.launch {
            flow {
                emit(repository.fetchConvInfo(conversationList))
            }
            .catchChatException {  }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    it?.forEach { item ->
                        EaseIM.getCache().insertConvInfo(item.id, item)
                    }
                    _view?.fetchConversationInfoByUserSuccess(it)
                }
            }
        }
    }

}