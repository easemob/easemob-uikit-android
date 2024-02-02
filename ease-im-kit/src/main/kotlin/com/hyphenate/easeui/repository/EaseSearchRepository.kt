package com.hyphenate.easeui.repository

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatContactManager
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatManager
import com.hyphenate.easeui.common.extensions.parse
import com.hyphenate.easeui.common.suspends.searchContact
import com.hyphenate.easeui.model.EaseConversation
import com.hyphenate.easeui.model.EaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EaseSearchRepository(
    private val chatManager: ChatManager = ChatClient.getInstance().chatManager(),
    private val chatContactManager: ChatContactManager = ChatClient.getInstance().contactManager(),
) {

    companion object {
        private const val TAG = "SearchRep"
    }

    /**
     * Search user from local .
     */
    suspend fun searchUser(query:String):MutableList<EaseUser> =
        withContext(Dispatchers.IO){
           chatContactManager.searchContact(query)
        }

    /**
     * Search conversation from local .
     */
    suspend fun searchConversation(query:String): List<EaseConversation> =
        withContext(Dispatchers.IO){
           chatManager.allConversationsBySort.filter { it ->
                   EaseIM.getConversationInfoProvider()?.getProfile(it.conversationId(), it.type)?.let {
                         it.name?.contains(query)
                   } ?: kotlin.run{
                       when(it.type) {
                           ChatConversationType.GroupChat -> {
                               ChatClient.getInstance().groupManager().getGroup(it.conversationId())?.groupName?.contains(query)
                                   ?: it.conversationId().contains(query)
                           }
                           ChatConversationType.ChatRoom -> {
                               ChatClient.getInstance().chatroomManager().getChatRoom(it.conversationId())?.name?.contains(query)
                                   ?: it.conversationId().contains(query)
                               it.conversationId().contains(query)
                           }
                           else -> {
                               it.conversationId().contains(query)
                           }
                       }
                   }
               }
               .map { it.parse() }
        }

}