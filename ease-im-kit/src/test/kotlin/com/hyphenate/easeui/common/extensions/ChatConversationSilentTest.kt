package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.common.ChatPushRemindType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatConversationSilentTest {

    @Test
    fun sdkRemindTypeIsAuthoritativeWhenAvailable() {
        assertFalse(resolveConversationSilent(ChatPushRemindType.ALL, legacyMuted = true))
        assertTrue(resolveConversationSilent(ChatPushRemindType.MENTION_ONLY, legacyMuted = false))
        assertTrue(resolveConversationSilent(ChatPushRemindType.NONE, legacyMuted = false))
    }

    @Test
    fun legacyCacheIsOnlyUsedWhenSdkConversationIsUnavailable() {
        assertTrue(resolveConversationSilent(null, legacyMuted = true))
        assertFalse(resolveConversationSilent(null, legacyMuted = false))
    }
}
