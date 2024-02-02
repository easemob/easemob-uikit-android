package com.hyphenate.easeui.repository

import android.content.Context
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.suspends.agreeInviteAction
import com.hyphenate.easeui.common.suspends.getAllSystemMessage
import com.hyphenate.easeui.common.suspends.refuseInviteAction
import com.hyphenate.easeui.feature.invitation.helper.EaseNotificationMsgManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EaseNotificationRepository(
    private val notificationMsgManager: EaseNotificationMsgManager = EaseNotificationMsgManager.getInstance()
) {

    companion object {
        private const val TAG = "NotificationRep"
    }

    suspend fun getAllMessage():List<ChatMessage> =
        withContext(Dispatchers.IO){
            notificationMsgManager.getAllSystemMessage()
        }

    suspend fun agreeInvite(context: Context,msg:ChatMessage):Int =
        withContext(Dispatchers.IO){
            notificationMsgManager.agreeInviteAction(context,msg)
        }

    suspend fun refuseInvite(context: Context,msg:ChatMessage):Int =
        withContext(Dispatchers.IO){
            notificationMsgManager.refuseInviteAction(context,msg)
        }
}