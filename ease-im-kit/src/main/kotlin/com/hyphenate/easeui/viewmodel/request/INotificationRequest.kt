package com.hyphenate.easeui.viewmodel.request

import android.content.Context
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.viewmodel.IAttachView

interface INotificationRequest : IAttachView {

    fun getAllMessage()

    fun agreeInvite(context: Context,msg:ChatMessage)

    fun refuseInvite(context: Context,msg:ChatMessage)

}