package com.hyphenate.easeui.common

import com.hyphenate.easeui.common.enums.ChatUIKitCacheType
import com.hyphenate.easeui.model.ChatUIKitProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatUIKitCacheTest {

    @Test
    fun providerFieldsTakePriorityAndSdkFillsMissingFields() {
        val cache = ChatUIKitCache()
        cache.insertUser(ChatUIKitProfile("alice", name = "Provider", remark = "Business remark"))
        cache.insertSdkUser(ChatUIKitProfile("alice", name = "SDK", avatar = "sdk-avatar"))

        val result = cache.getUser("alice")

        assertEquals("Provider", result?.name)
        assertEquals("sdk-avatar", result?.avatar)
        assertEquals("Business remark", result?.remark)
    }

    @Test
    fun sdkUpdateDoesNotOverwriteProviderFields() {
        val cache = ChatUIKitCache()
        cache.insertUser(ChatUIKitProfile("alice", name = "Provider", avatar = "provider-avatar"))
        cache.insertSdkUser(ChatUIKitProfile("alice", name = "SDK 1", avatar = "sdk-1"))
        cache.insertSdkUser(ChatUIKitProfile("alice", name = "SDK 2", avatar = "sdk-2"))

        val result = cache.getUser("alice")

        assertEquals("Provider", result?.name)
        assertEquals("provider-avatar", result?.avatar)
    }

    @Test
    fun groupNameCardsAreIsolatedAndLatestMessageWins() {
        val cache = ChatUIKitCache()
        cache.insertGroupNameCard("group-1", "alice", "old", 10)
        cache.insertGroupNameCard("group-1", "alice", "older", 9)
        cache.insertGroupNameCard("group-2", "alice", "other group", 8)

        assertEquals("old", cache.getGroupNameCard("group-1", "alice")?.nameCard)
        assertEquals("other group", cache.getGroupNameCard("group-2", "alice")?.nameCard)

        cache.insertGroupNameCard("group-1", "alice", null, 11)
        assertNull(cache.getGroupNameCard("group-1", "alice")?.nameCard)
    }

    @Test
    fun conversationCacheClearRemovesGroupNameCards() {
        val cache = ChatUIKitCache()
        cache.insertGroupNameCard("group-1", "alice", "card", 10)

        cache.clear(ChatUIKitCacheType.CONVERSATION_INFO)

        assertNull(cache.getGroupNameCard("group-1", "alice"))
    }

    @Test
    fun legacyMessageProfileKeepsNewestTimestamp() {
        val cache = ChatUIKitCache()
        cache.insertMessageUser("alice", ChatUIKitProfile("alice", name = "new").apply { setTimestamp(20) })
        cache.insertMessageUser("alice", ChatUIKitProfile("alice", name = "old").apply { setTimestamp(10) })

        assertEquals("new", cache.getMessageUserInfo("alice")?.name)
    }
}
