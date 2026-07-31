package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.common.ChatUIKitConstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageCompatibilityUserInfoTest {

    @Test
    fun compatibilityUserInfoUsesLegacyMessageExtensionFormat() {
        val attributes = buildCompatibilityUserInfoAttributes(
            "Alice",
            "https://example.com/alice.png",
            "Friend",
        )

        assertEquals("Alice", attributes[ChatUIKitConstant.MESSAGE_EXT_USER_INFO_NICKNAME_KEY])
        assertEquals(
            "https://example.com/alice.png",
            attributes[ChatUIKitConstant.MESSAGE_EXT_USER_INFO_AVATAR_KEY],
        )
        assertEquals("Friend", attributes[ChatUIKitConstant.MESSAGE_EXT_USER_INFO_REMARK_KEY])
    }

    @Test
    fun compatibilityUserInfoOnlyIncludesNonEmptyFields() {
        val attributes = buildCompatibilityUserInfoAttributes("Alice", null)

        assertEquals("Alice", attributes[ChatUIKitConstant.MESSAGE_EXT_USER_INFO_NICKNAME_KEY])
        assertFalse(attributes.containsKey(ChatUIKitConstant.MESSAGE_EXT_USER_INFO_AVATAR_KEY))
        assertFalse(attributes.containsKey(ChatUIKitConstant.MESSAGE_EXT_USER_INFO_REMARK_KEY))
    }

    @Test
    fun compatibilityUserInfoIsNotAddedWhenAllFieldsAreEmpty() {
        val attributes = buildCompatibilityUserInfoAttributes(null, "", null)

        assertTrue(attributes.isEmpty())
    }
}
