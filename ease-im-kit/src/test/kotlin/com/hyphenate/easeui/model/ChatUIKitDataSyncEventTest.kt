package com.hyphenate.easeui.model

import com.hyphenate.chat.EMOptions.EMDataSyncType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatUIKitDataSyncEventTest {

    @Test
    fun syncStateCarriesFinishErrorOnlyWhenProvided() {
        val started = ChatUIKitDataSyncEvent(
            EMDataSyncType.CONVERSATIONS,
            ChatUIKitDataSyncEvent.STATE.STARTED,
        )
        val finished = ChatUIKitDataSyncEvent(
            EMDataSyncType.CONVERSATIONS,
            ChatUIKitDataSyncEvent.STATE.FINISHED,
            500,
        )

        assertNull(started.errorCode)
        assertEquals(500, finished.errorCode)
    }

    @Test
    fun eventKeysAreStableAndIndependent() {
        val conversationKey = ChatUIKitDataSyncEvent.eventKey(EMDataSyncType.CONVERSATIONS)
        val contactKey = ChatUIKitDataSyncEvent.eventKey(EMDataSyncType.CONTACTS)
        val groupKey = ChatUIKitDataSyncEvent.eventKey(EMDataSyncType.JOINED_GROUPS)

        assertEquals("DATA_SYNC/CONVERSATIONS", conversationKey)
        assertNotEquals(conversationKey, contactKey)
        assertNotEquals(contactKey, groupKey)
    }

    @Test
    fun sdkTypesMapToUIKitDataTypes() {
        assertEquals(ChatUIKitEvent.TYPE.CONVERSATION, EMDataSyncType.CONVERSATIONS.toUIKitEventType())
        assertEquals(ChatUIKitEvent.TYPE.CONTACT, EMDataSyncType.CONTACTS.toUIKitEventType())
        assertEquals(ChatUIKitEvent.TYPE.GROUP, EMDataSyncType.JOINED_GROUPS.toUIKitEventType())
        assertNull(EMDataSyncType.NONE.toUIKitEventType())
        assertNull((null as EMDataSyncType?).toUIKitEventType())
    }
}
