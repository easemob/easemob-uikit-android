package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.model.ChatUIKitProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageUserProfileResolverTest {

    @Test
    fun providerFieldsWinAndSdkFillsIndependently() {
        val result = resolveMessageUserProfile(
            userId = "alice",
            providerProfile = ChatUIKitProfile("alice", name = "Provider"),
            sdkProfile = ChatUIKitProfile("alice", name = "SDK", avatar = "sdk-avatar"),
            senderNickname = "Sender",
            senderAvatar = "sender-avatar",
            senderRemark = "sender-remark",
            groupNameCard = null,
            legacyProfile = ChatUIKitProfile("alice", name = "Legacy", avatar = "legacy-avatar"),
            isGroupChat = false,
        )

        assertEquals("Provider", result.name)
        assertEquals("sdk-avatar", result.avatar)
        assertEquals("sender-remark", result.remark)
    }

    @Test
    fun groupNameCardHasHighestDisplayNamePriority() {
        val result = resolveMessageUserProfile(
            userId = "alice",
            providerProfile = ChatUIKitProfile("alice", name = "Provider", remark = "Provider remark"),
            sdkProfile = ChatUIKitProfile("alice", name = "SDK", remark = "SDK remark"),
            senderNickname = "Sender",
            senderAvatar = "sender-avatar",
            senderRemark = "Sender remark",
            groupNameCard = "Group card",
            legacyProfile = null,
            isGroupChat = true,
        )

        assertEquals("Group card", result.remark)
        assertEquals("Provider", result.name)
    }

    @Test
    fun legacyProfileAndIdRemainAvailableAsFallbacks() {
        val legacy = resolveMessageUserProfile(
            userId = "alice",
            providerProfile = null,
            sdkProfile = null,
            senderNickname = null,
            senderAvatar = null,
            senderRemark = null,
            groupNameCard = null,
            legacyProfile = ChatUIKitProfile("alice", name = "Legacy"),
            isGroupChat = false,
        )
        val idOnly = resolveMessageUserProfile(
            userId = "alice",
            providerProfile = null,
            sdkProfile = null,
            senderNickname = null,
            senderAvatar = null,
            senderRemark = null,
            groupNameCard = null,
            legacyProfile = null,
            isGroupChat = false,
        )

        assertEquals("Legacy", legacy.getRemarkOrName())
        assertEquals("alice", idOnly.getRemarkOrName())
    }
}
