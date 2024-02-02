package com.hyphenate.easeui.demo

import com.hyphenate.easeui.feature.chat.EaseChatFragment
import com.hyphenate.easeui.feature.chat.activities.EaseChatActivity

class ChatActivity: EaseChatActivity() {

    override fun setChildSettings(builder: EaseChatFragment.Builder) {
        super.setChildSettings(builder)
        builder.setCustomFragment(ChatFragment())
    }
}