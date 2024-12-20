package com.hyphenate.easeui.model

import com.hyphenate.easeui.feature.chat.enums.ChatUIKitReactionType

data class ChatUIKitReaction (
    var identityCode: String? = null,
    var icon: Int = 0,
    var emojiText: String? = null,
    var count: Int = 0,
    var isAddedBySelf: Boolean = false,
    var type: ChatUIKitReactionType = ChatUIKitReactionType.NORMAL
)
