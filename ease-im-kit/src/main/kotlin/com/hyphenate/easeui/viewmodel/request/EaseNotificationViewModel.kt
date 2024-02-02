package com.hyphenate.easeui.viewmodel.request

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.collectWithCheckErrorCode
import com.hyphenate.easeui.feature.invitation.interfaces.IEaseNotificationResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.repository.EaseNotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EaseNotificationViewModel (
    private val stopTimeoutMillis: Long = 5000
): ViewModel(),INotificationRequest{

    private var _view: IEaseNotificationResultView? = null

    private val repository: EaseNotificationRepository = EaseNotificationRepository()

    override fun attachView(view: IControlDataView) {
        _view = view as IEaseNotificationResultView
    }

    override fun getAllMessage() {
        viewModelScope.launch {
            flow {
                emit(repository.getAllMessage())
            }
            .catchChatException { e ->
                _view?.getAllMessageFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.getAllMessageSuccess(it)
                }
            }
        }
    }

    override fun agreeInvite(context: Context,msg: ChatMessage) {
        viewModelScope.launch {
            flow {
                emit(repository.agreeInvite(context, msg))
            }
            .catchChatException { e ->
                _view?.agreeInviteFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.agreeInviteSuccess()
            }
        }
    }

    override fun refuseInvite(context: Context,msg: ChatMessage) {
        viewModelScope.launch {
            flow {
                emit(repository.refuseInvite(context, msg))
            }
            .catchChatException { e ->
                _view?.refuseInviteFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.refuseInviteSuccess()
            }
        }
    }

}