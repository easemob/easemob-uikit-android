package com.hyphenate.easeui.viewmodel.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatGroupManager
import com.hyphenate.easeui.common.ChatGroupOptions
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.collectWithCheckErrorCode
import com.hyphenate.easeui.common.extensions.parse
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.ContactSortedHelper
import com.hyphenate.easeui.feature.group.interfaces.IEaseGroupResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.model.setUserInitialLetter
import com.hyphenate.easeui.provider.getSyncMemberProfile
import com.hyphenate.easeui.repository.EaseConversationRepository
import com.hyphenate.easeui.repository.EaseGroupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class EaseGroupViewModel(
    private val groupManager: ChatGroupManager = ChatClient.getInstance().groupManager(),
    private val stopTimeoutMillis: Long = 5000
): ViewModel(),IGroupRequest{
    private var _view: IEaseGroupResultView? = null

    private val repository:EaseGroupRepository = EaseGroupRepository(groupManager)
    private val convRepository: EaseConversationRepository = EaseConversationRepository()

    override fun attachView(view: IControlDataView) {
        _view = view as IEaseGroupResultView
    }

    override fun loadJoinedGroupData(page:Int) {
        viewModelScope.launch {
            flow {
                emit(repository.loadJoinedGroupData(page))
            }
            .catchChatException { e ->
                _view?.loadGroupListFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.loadGroupListSuccess(it.toMutableList())
                }
            }
        }
    }

    override fun loadLocalJoinedGroupData() {
        viewModelScope.launch {
            flow {
                emit(repository.loadLocalJoinedGroupData())
            }
                .catchChatException { e ->
                    _view?.loadLocalGroupListFail(e.errorCode, e.description)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
                .collect {
                    if (it != null) {
                        _view?.loadLocalGroupListSuccess(it.toMutableList())
                    }
                }
        }
    }

    override fun createGroup(
        groupName: String,
        desc: String,
        members: MutableList<String>,
        reason: String,
        options: ChatGroupOptions
    ) {
        viewModelScope.launch {
            flow {
                emit(repository.createGroup(groupName, desc, members, reason, options))
            }
            .catchChatException { e ->
                _view?.createGroupFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                it?.let { it1 -> _view?.createGroupSuccess(it1) }
            }
        }
    }

    override fun fetchGroupMemberFromService(groupId: String) {
        viewModelScope.launch {
            flow {
                emit(repository.fetGroupMemberFromService(groupId))
            }
            .catchChatException { e ->
                _view?.fetchGroupMemberFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), mutableListOf())
            .collect {
                val data = it.map { user->
                    EaseIM.getGroupMemberProfileProvider()?.getSyncMemberProfile(groupId,user.userId)?.toUser()?: EaseUser(user.userId)
                }
                data.map { user->
                    user.setUserInitialLetter()
                }
                val sortedList = ContactSortedHelper.sortedList(data)
                _view?.fetchGroupMemberSuccess(sortedList)
            }
        }
    }

    override fun loadLocalMember(groupId: String) {
        viewModelScope.launch {
            flow {
                emit(repository.loadLocalMember(groupId))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), mutableListOf())
            .collect {
                it.let { it1 -> _view?.loadLocalMemberSuccess(it1) }
            }
        }
    }

    override fun fetchGroupDetails(groupId: String) {
        viewModelScope.launch {
            flow {
                emit(repository.fetchGroupDetails(groupId))
            }
            .catchChatException { e ->
                _view?.fetchGroupDetailFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                it?.let { it1 -> _view?.fetchGroupDetailSuccess(it1) }
            }
        }
    }

    override fun addGroupMember(groupId: String, members: MutableList<String>) {
        viewModelScope.launch {
            flow {
                emit(repository.addGroupMember(groupId,members))
            }
            .catchChatException { e ->
                _view?.addGroupMemberFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.addGroupMemberSuccess()
            }
        }
    }

    override fun removeGroupMember(groupId: String, members: MutableList<String>) {
        viewModelScope.launch {
            flow {
                emit(repository.removeGroupMember(groupId,members))
            }
            .catchChatException { e ->
                _view?.removeChatGroupMemberFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.removeChatGroupMemberSuccess()
            }
        }
    }

    override fun leaveChatGroup(groupId: String) {
        viewModelScope.launch {
            flow {
                emit(repository.leaveChatGroup(groupId))
            }
            .catchChatException { e ->
                _view?.leaveChatGroupFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.leaveChatGroupSuccess()
            }
        }
    }

    override fun destroyChatGroup(groupId: String) {
        viewModelScope.launch {
            flow {
                emit(repository.destroyChatGroup(groupId))
            }
            .catchChatException { e ->
                _view?.destroyChatGroupFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.destroyChatGroupSuccess()
            }
        }
    }

    override fun changeChatGroupName(groupId: String, newName: String) {
        viewModelScope.launch {
            flow {
                emit(repository.changeChatGroupName(groupId, newName))
            }
            .catchChatException { e ->
                _view?.changeChatGroupNameFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.changeChatGroupNameSuccess()
            }
        }
    }

    override fun changeChatGroupDescription(groupId: String, description: String) {
        viewModelScope.launch {
            flow {
                emit(repository.changeChatGroupDescription(groupId, description))
            }
            .catchChatException { e ->
                _view?.changeChatGroupDescriptionFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
            .collectWithCheckErrorCode {
                _view?.changeChatGroupDescriptionSuccess()
            }
        }
    }

    override fun changeChatGroupOwner(groupId: String, newOwner: String) {
        viewModelScope.launch {
            flow {
                emit(repository.changeChatGroupOwner(groupId, newOwner))
            }
            .catchChatException { e ->
                _view?.changeChatGroupOwnerFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.EM_NO_ERROR)
            .collectWithCheckErrorCode {
                _view?.changeChatGroupOwnerSuccess()
            }
        }
    }

    override fun fetchGroupMemberAllAttributes(
        groupId: String,
        userList: List<String>,
        keyList: List<String>,
    ) {
        viewModelScope.launch {
            flow {
                emit(repository.fetchMemberAllAttributes(groupId,userList,keyList))
            }
            .catchChatException { e ->
                _view?.getGroupMemberAllAttributesFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
            .collect {
                if (it != null) {
                    _view?.getGroupMemberAllAttributesSuccess(it)
                }
            }
        }
    }

    override fun setGroupMemberAttributes(groupId: String, userId: String,attribute:MutableMap<String,String>) {
        viewModelScope.launch {
            flow {
                emit(repository.setGroupMemberAttributes(groupId, userId,attribute))
            }
            .catchChatException { e ->
                _view?.setGroupMemberAttributesFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR
            ).collectWithCheckErrorCode {
                _view?.setGroupMemberAttributesSuccess()
            }
        }
    }

    override fun fetchMemberInfo(members: MutableMap<String,MutableList<String>>?) {
        if (members == null) return
        viewModelScope.launch {
            flow {
                emit(repository.fetchMemberInfo(members))
            }
            .catchChatException { e ->
                _view?.fetchMemberInfoFail(e.errorCode, e.description)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null
            ).collect {
                if (it != null) {
                    it.forEach { item ->
                        EaseIM.getCache().insertGroupUser(item.key, item.value.toUser())
                    }
                    _view?.fetchMemberInfoSuccess(it)
                }
            }
        }
    }


    override fun deleteConversation(conversationId: String?) {
        viewModelScope.launch {
            ChatClient.getInstance().chatManager().getConversation(conversationId,ChatConversationType.GroupChat)?.parse()?.let {
                flow {
                    emit(convRepository.deleteConversation(it))
                }
                    .catchChatException { e ->
                        _view?.deleteConversationByGroupFail(e.errorCode,e.description)
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), ChatError.GENERAL_ERROR)
                    .collectWithCheckErrorCode {
                        _view?.deleteConversationByGroupSuccess(conversationId)
                    }
            } ?: _view?.deleteConversationByGroupFail(ChatError.INVALID_PARAM,"conversation is null")
        }
    }

    override fun makeSilentModeForConversation(
        conversationId: String,
        conversationType:ChatConversationType
    ) {
        viewModelScope.launch {
            flow {
                emit(convRepository.makeSilentForConversation(conversationId,conversationType))
            }
                .catchChatException { e ->
                    _view?.makeSilentForGroupFail(e.errorCode, e.description)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
                .collect {
                    if (it != null) {
                        ChatLog.e("conversation", "makeSilentForGroupSuccess")
                        _view?.makeSilentForGroupSuccess(it)
                    }
                }
        }

    }

    override fun cancelSilentForConversation(
        conversationId: String,
        conversationType: ChatConversationType
    ) {
        viewModelScope.launch {
            flow {
                emit(convRepository.cancelSilentForConversation(conversationId,conversationType))
            }
                .catchChatException { e ->
                    _view?.cancelSilentForGroupFail(e.errorCode, e.description)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
                .collect {
                    if (it != null) {
                        ChatLog.e("conversation", "cancelSilentForGroupSuccess")
                        _view?.cancelSilentForGroupSuccess()
                    }
                }
        }
    }


}