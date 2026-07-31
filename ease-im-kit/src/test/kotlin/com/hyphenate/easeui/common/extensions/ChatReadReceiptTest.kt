package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.common.ChatMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReadReceiptTest {

    @Test
    fun onlyEligibleUnreadReceivedMessagesAreSelected() {
        assertTrue(shouldSendHistoricalReadReceipt(true, false, true, true, ChatMessageType.TXT))
        assertFalse(shouldSendHistoricalReadReceipt(false, false, true, true, ChatMessageType.TXT))
        assertFalse(shouldSendHistoricalReadReceipt(true, true, true, true, ChatMessageType.TXT))
        assertFalse(shouldSendHistoricalReadReceipt(true, false, false, true, ChatMessageType.TXT))
    }

    @Test
    fun singleChatAttachmentsKeepClickToReadBehavior() {
        assertFalse(shouldSendHistoricalReadReceipt(true, false, true, true, ChatMessageType.VIDEO))
        assertFalse(shouldSendHistoricalReadReceipt(true, false, true, true, ChatMessageType.VOICE))
        assertFalse(shouldSendHistoricalReadReceipt(true, false, true, true, ChatMessageType.FILE))
        assertTrue(shouldSendHistoricalReadReceipt(true, false, true, false, ChatMessageType.FILE))
    }

    @Test
    fun receiptsAreChunkedAtSdkLimit() {
        assertEquals(emptyList<List<Int>>(), chunkReadReceipts<Int>(emptyList()))
        assertEquals(listOf(1), chunkReadReceipts(listOf(1)).map { it.size })
        assertEquals(listOf(50), chunkReadReceipts((1..50).toList()).map { it.size })
        assertEquals(listOf(50, 1), chunkReadReceipts((1..51).toList()).map { it.size })
        assertEquals(listOf(50, 50), chunkReadReceipts((1..100).toList()).map { it.size })
    }

    @Test
    fun historicalUnreadMessagesAreCollectedAcrossDatabasePages() {
        val messages = (500 downTo 1).toList()
        val cursors = mutableListOf<Long>()

        val result = collectHistoricalUnreadItems(
            unreadTarget = 450,
            loadPage = { cursor, pageSize ->
                cursors.add(cursor)
                messages.filter { cursor < 0 || it < cursor }.take(pageSize)
            },
            timestampOf = { it.toLong() },
            uniqueKeyOf = { it.toString() },
            isUnreadReceived = { true },
            shouldInclude = { true },
        )

        assertEquals(450, result.size)
        assertEquals(listOf(-1L, 101L), cursors)
        assertEquals(51, result.last())
    }
}
