package com.hyphenate.easeui.repository

import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatContactManager
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatManager
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.ChatSearchScope
import com.hyphenate.easeui.common.extensions.getDisplayInfo
import com.hyphenate.easeui.common.extensions.parse
import com.hyphenate.easeui.common.suspends.searchBlockContact
import com.hyphenate.easeui.common.suspends.searchContact
import com.hyphenate.easeui.common.suspends.searchMessage
import com.hyphenate.easeui.model.ChatUIKitConversation
import com.hyphenate.easeui.model.ChatUIKitUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatUIKitSearchRepository(
    private val chatManager: ChatManager = ChatClient.getInstance().chatManager(),
    private val chatContactManager: ChatContactManager = ChatClient.getInstance().contactManager(),
) {

    companion object {
        private const val TAG = "SearchRep"
    }

    /**
     * Search user from local .
     */
    suspend fun searchUser(query:String):MutableList<ChatUIKitUser> =
        withContext(Dispatchers.IO){
           chatContactManager.searchContact(query)
        }

    /**
     * Search block user from local .
     */
    suspend fun searchBlockUser(query:String):MutableList<ChatUIKitUser> =
        withContext(Dispatchers.IO){
            chatContactManager.searchBlockContact(query)
        }

    /**
     * Search conversation from local .
     */
    suspend fun searchConversation(query:String): List<ChatUIKitConversation> =
        withContext(Dispatchers.IO){
           chatManager.allConversationsBySort.filter {
                   it.getDisplayInfo().name.contains(query) || it.conversationId().contains(query)
               }
               .map { it.parse() }
        }

    /**
     * Search msg from local .
     */
    suspend fun searchMessage(
        keywords:String,
        timeStamp:Long,
        maxCount:Int,
        from:String?,
        direction:ChatSearchDirection,
        chatScope:ChatSearchScope
    ):List<ChatMessage> =
        withContext(Dispatchers.IO){
            chatManager.searchMessage(keywords, timeStamp, maxCount, from, direction,chatScope)
        }

    /**
     * Search conversation msg from local .
     */
    suspend fun searchMessageByConversation(
        conversationId:String,
        keywords:String,
        timeStamp:Long,
        maxCount:Int,
        from:String?,
        direction:ChatSearchDirection,
        chatScope:ChatSearchScope
    ):List<ChatMessage> =
        withContext(Dispatchers.IO){
            chatManager.getConversation(
                conversationId,
                ChatConversationType.Chat,
                true
            ).searchMessage(
                keywords,timeStamp,maxCount,from,direction,chatScope
            )
        }

}
