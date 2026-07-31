package com.hyphenate.easeui.configs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUIKitConfigTest {

    @Test
    fun userInfoCompatibilityModeIsDisabledByDefault() {
        assertFalse(ChatUIKitConfig().compatibilityModeForUserInfo)
    }

    @Test
    fun userInfoCompatibilityModeCanBeEnabled() {
        val config = ChatUIKitConfig().apply {
            compatibilityModeForUserInfo = true
        }

        assertTrue(config.compatibilityModeForUserInfo)
    }
}
