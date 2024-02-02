package com.hyphenate.easeui.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.feature.search.interfaces.IEaseSearchResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.repository.EaseSearchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class EaseSearchViewModel(
    private val stopTimeoutMillis: Long = 5000
): ViewModel(),IEaseSearchRequest{
    private var _view: IEaseSearchResultView? = null

    private val repository: EaseSearchRepository = EaseSearchRepository()

    override fun attachView(view: IControlDataView) {
        _view = view as IEaseSearchResultView
    }

    override fun searchUser(query: String) {
        viewModelScope.launch {
            flow {
                emit(repository.searchUser(query))
            }
            .catchChatException { }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.searchSuccess(it)
                }
            }
        }
    }

    override fun searchConversation(query: String) {
        viewModelScope.launch {
            flow {
                emit(repository.searchConversation(query))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.searchSuccess(it)
                }
            }
        }
    }


}