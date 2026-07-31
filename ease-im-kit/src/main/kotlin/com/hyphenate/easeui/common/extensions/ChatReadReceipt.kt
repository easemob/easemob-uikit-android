package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.common.ChatMessageType

internal const val MAX_READ_RECEIPTS_PER_BATCH = 50
internal const val HISTORY_READ_RECEIPT_QUERY_PAGE_SIZE = 400

internal fun shouldSendHistoricalReadReceipt(
    isReceived: Boolean,
    isRead: Boolean,
    isNeedReadReceipt: Boolean,
    isSingleChat: Boolean,
    messageType: ChatMessageType,
): Boolean {
    if (!isReceived || isRead || !isNeedReadReceipt) return false
    if (!isSingleChat) return true
    return messageType != ChatMessageType.VIDEO &&
        messageType != ChatMessageType.VOICE &&
        messageType != ChatMessageType.FILE
}

internal fun <T> chunkReadReceipts(messages: List<T>): List<List<T>> =
    messages.chunked(MAX_READ_RECEIPTS_PER_BATCH)

internal fun <T> collectHistoricalUnreadItems(
    unreadTarget: Int,
    loadPage: (cursor: Long, pageSize: Int) -> List<T>,
    timestampOf: (T) -> Long,
    uniqueKeyOf: (T) -> String,
    isUnreadReceived: (T) -> Boolean,
    shouldInclude: (T) -> Boolean,
): List<T> {
    if (unreadTarget <= 0) return emptyList()
    val result = mutableListOf<T>()
    val seenKeys = HashSet<String>()
    var observedUnread = 0
    var cursor = -1L
    while (observedUnread < unreadTarget) {
        val page = loadPage(cursor, HISTORY_READ_RECEIPT_QUERY_PAGE_SIZE)
        if (page.isEmpty()) break
        for (item in page) {
            if (!seenKeys.add(uniqueKeyOf(item))) continue
            if (!isUnreadReceived(item)) continue
            observedUnread++
            if (shouldInclude(item)) result.add(item)
            if (observedUnread >= unreadTarget) break
        }
        if (observedUnread >= unreadTarget || page.size < HISTORY_READ_RECEIPT_QUERY_PAGE_SIZE) break
        val nextCursor = page.minOf(timestampOf)
        if (nextCursor == cursor) break
        cursor = nextCursor
    }
    return result
}
