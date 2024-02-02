package com.hyphenate.easeui.demo

import android.os.Bundle
import com.hyphenate.easeui.common.extensions.showToast
import com.hyphenate.easeui.feature.chat.EaseChatFragment

class ChatFragment: EaseChatFragment() {

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.titleBar?.inflateMenu(R.menu.demo_chat_menu)
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
                else -> false
            }
        }
    }
}