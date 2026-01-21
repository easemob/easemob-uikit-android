package com.hyphenate.easeui.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * 用于“第一片流式数据先到，但列表里还没插入该消息 item”的场景：
 * - onStreamMessageReceived 找不到 msgId 对应的 adapter item 时，把 chunk 先暂存；
 * - 等该 msgId 的 View bind（onSetUpView）时，再一次性 consume 出来给打字机追加。
 *
 * 注意：这里不用于“消息已在列表但当前不可见”的场景（那类不强制打字机，避免用户滚动回来时从中间重新打）。
 */
internal object StreamMarkdownChunkStore {
    private val pending = ConcurrentHashMap<String, StringBuilder>()

    fun append(msgId: String, chunk: String) {
        if (msgId.isBlank() || chunk.isEmpty()) return
        pending.compute(msgId) { _, old ->
            val sb = old ?: StringBuilder()
            sb.append(chunk)
            sb
        }
    }

    fun consume(msgId: String): String? {
        if (msgId.isBlank()) return null
        return pending.remove(msgId)?.toString()
    }

    fun hasPending(msgId: String): Boolean {
        if (msgId.isBlank()) return false
        return pending.containsKey(msgId)
    }
}


