package com.hyphenate.easeui.viewmodel.search

import com.hyphenate.easeui.viewmodel.IAttachView

interface IEaseSearchRequest: IAttachView {

    fun searchUser(query:String)

    fun searchConversation(query:String)
}