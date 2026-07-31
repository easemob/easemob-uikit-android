package com.hyphenate.easeui.common.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatConversationDisplayInfoTest {

    @Test
    fun singleConversation_providerFieldsTakePriority() {
        val result = resolveSingleDisplayInfo(
            conversationId = "alice",
            providerRemark = "备注",
            providerName = "Provider Name",
            providerAvatar = "provider-avatar",
            sdkName = "SDK Name",
            sdkAvatar = "sdk-avatar",
            legacyName = "Legacy Name",
        )

        assertEquals("备注", result.name)
        assertEquals("provider-avatar", result.avatar)
    }

    @Test
    fun singleConversation_emptyProviderFieldsFallBackIndependently() {
        val result = resolveSingleDisplayInfo(
            conversationId = "alice",
            providerRemark = "",
            providerName = null,
            providerAvatar = "provider-avatar",
            sdkName = "SDK Name",
            sdkAvatar = "sdk-avatar",
            legacyName = "Legacy Name",
        )

        assertEquals("SDK Name", result.name)
        assertEquals("provider-avatar", result.avatar)
    }

    @Test
    fun singleConversation_missingSyncedDataFallsBackToLegacyAndId() {
        val legacyResult = resolveSingleDisplayInfo(
            conversationId = "alice",
            providerRemark = null,
            providerName = null,
            providerAvatar = null,
            sdkName = "",
            sdkAvatar = "",
            legacyName = "Legacy Name",
        )
        val idResult = resolveSingleDisplayInfo(
            conversationId = "alice",
            providerRemark = null,
            providerName = null,
            providerAvatar = null,
            sdkName = "",
            sdkAvatar = "",
            legacyName = null,
        )

        assertEquals("Legacy Name", legacyResult.name)
        assertNull(legacyResult.avatar)
        assertEquals("alice", idResult.name)
    }

    @Test
    fun groupConversation_usesProviderThenSdkThenLocal() {
        val sdkResult = resolveGroupDisplayInfo(
            conversationId = "group-1",
            providerName = "",
            providerAvatar = null,
            sdkName = "SDK Group",
            sdkAvatar = "sdk-avatar",
            localName = "Local Group",
            localAvatar = "local-avatar",
        )

        assertEquals("SDK Group", sdkResult.name)
        assertEquals("sdk-avatar", sdkResult.avatar)
    }

    @Test
    fun chatRoom_usesLocalNameAndHasNoConversationAvatar() {
        val localResult = resolveChatRoomDisplayInfo("room-1", "Room Name")
        val fallbackResult = resolveChatRoomDisplayInfo("room-1", "")

        assertEquals("Room Name", localResult.name)
        assertNull(localResult.avatar)
        assertEquals("room-1", fallbackResult.name)
    }
}
