package com.hyphenate.easeui.demo

import android.os.Bundle
import com.hyphenate.easeui.common.extensions.showToast
import com.hyphenate.easeui.feature.chat.UIKitChatFragment
import com.hyphenate.easeui.feature.chat.enums.ChatUIKitType
import com.hyphenate.easeui.feature.thread.ChatUIKitThreadListActivity

class ChatFragment: UIKitChatFragment() {

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.titleBar?.inflateMenu(R.menu.demo_chat_menu)
        if (chatType == ChatUIKitType.SINGLE_CHAT){
            binding?.titleBar?.setMenuIconVisible(R.id.chat_menu_thread,false)
        }
        setMenuListener()
    }

    private fun setMenuListener() {
        binding?.titleBar?.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.chat_menu_voice_call -> {
                    mContext.showToast("voice call")
                    true
                }
                R.id.chat_menu_video_call -> {
                    mContext.showToast("video call")
                    true
                }
                R.id.chat_menu_thread -> {
                    ChatUIKitThreadListActivity.actionStart(mContext,conversationId)
                    true
                }
                else -> false
            }
        }
    }

    override fun cancelMultipleSelectStyle() {
        super.cancelMultipleSelectStyle()
        setMenuListener()
    }
}