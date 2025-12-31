package com.hyphenate.easeui.widget

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.AsyncDrawableSpan
import kotlin.jvm.java
import kotlin.math.max
import kotlin.math.min

/**
 * Streaming markdown -> throttled Markwon rendering -> typewriter reveal.
 *
 * Threading:
 * - [.append] is safe to call from any thread.
 * - Rendering/typewriter all happen on the provided main [Handler].
 */
class StreamingMarkdownTypewriter(
    private val mainHandler: Handler,
    markwon: Markwon,
    markdownView: TextView
) {
    private val markwon: Markwon
    private val markdownView: TextView

    // source+render state
    private val markdownSource = StringBuilder()
    private var renderedMarkdown: Spanned? = null
    private var renderedSourceLength = 0
    private var typedIndex = 0

    // scheduling state
    private var typewriterRunnable: Runnable? = null
    private var lastAsyncDrawableSpanCount = 0
    private var schedulePending = false
    private var lastScheduleUptimeMs: Long = 0
    private var renderPending = false
    private var lastRenderUptimeMs: Long = 0

    private val renderRunnable: Runnable = object : Runnable {
        override fun run() {
            // 这是一次“全量渲染”：把当前累积的 markdownSource 整体转成 Spanned（代价较高）
            renderPending = false
            lastRenderUptimeMs = SystemClock.uptimeMillis()
            renderedMarkdown = markwon.toMarkdown(markdownSource.toString())
            // 记录“本次渲染时源字符串长度”，用于后面判断是否有新数据进来需要再次渲染
            renderedSourceLength = markdownSource.length
            // typedIndex 是按“渲染后的 Spanned 长度”推进的：渲染后可能变长/变短，所以要 clamp
            typedIndex = min(typedIndex, renderedMarkdown!!.length)
            // 渲染完成后，确保打字机在跑（如果之前停在末尾，会自动续写）
            ensureTypewriterRunning()
        }
    }

    private val scheduleRunnable: Runnable = object : Runnable {
        override fun run() {
            schedulePending = false
            lastScheduleUptimeMs = SystemClock.uptimeMillis()
            // 打字机是手动 setText，Markwon 的插件回调不会自动触发
            // 所以图片/latex 等 AsyncDrawableSpan 必须我们主动 schedule 才会加载/刷新
            AsyncDrawableScheduler.schedule(markdownView)
        }
    }

    init {
        this.markwon = markwon
        this.markdownView = markdownView
    }

    fun reset() {
        stopAll()
        AsyncDrawableScheduler.unschedule(markdownView)
        markdownView.setText("")

        markdownSource.setLength(0)
        renderedMarkdown = null
        renderedSourceLength = 0
        typedIndex = 0

        lastAsyncDrawableSpanCount = 0
        lastScheduleUptimeMs = 0L
        schedulePending = false

        lastRenderUptimeMs = 0L
        renderPending = false
    }

    /** Append markdown chunk. Safe from any thread.  */
    fun append(chunkData: String?) {
        if (chunkData == null || chunkData.isEmpty()) {
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // 允许从任意线程调用：非主线程时切回主线程做 UI/状态更新
            mainHandler.post(Runnable { append(chunkData) })
            return
        }

        // 核心：只追加源 markdown（字符串缓冲），不要在这里直接 setText
        markdownSource.append(chunkData)

        // Key jitter reduction:
        // If we are still typing existing rendered content, don't swap renderedMarkdown yet.
        // Wait until we reach current end, then render the new full markdown and continue.
        if (renderedMarkdown == null || typedIndex >= renderedMarkdown!!.length) {
            // 只有“打到当前末尾”才触发全量渲染：避免打字过程中重排导致 UI 抖动
            requestRender(RENDER_THROTTLE_MS)
        }
    }

    fun destroy() {
        stopAll()
        AsyncDrawableScheduler.unschedule(markdownView)
    }

    private fun requestRender(throttleMs: Long) {
        val now = SystemClock.uptimeMillis()
        val sinceLast = now - lastRenderUptimeMs
        // 节流：短时间内多次 append，只合并成一次 render（减少卡顿）
        val delay = max(0L, throttleMs - sinceLast)
        if (renderPending) {
            return
        }
        renderPending = true
        mainHandler.postDelayed(renderRunnable, delay)
    }

    private fun ensureTypewriterRunning() {
        if (renderedMarkdown == null) {
            return
        }
        val total = renderedMarkdown!!.length
        if (typedIndex >= total) {
            requestSchedule(0L)
            return
        }
        if (typewriterRunnable != null) {
            return
        }

        typewriterRunnable = object : Runnable {
            override fun run() {
                if (renderedMarkdown == null) {
                    stopTypewriterOnly()
                    return
                }

                val total = renderedMarkdown!!.length
                if (typedIndex >= total) {
                    // If new source arrived while we were typing, render now and continue.
                    if (markdownSource.length > renderedSourceLength) {
                        // 说明“打字过程中有人 append 了新数据”，此刻补一次 render，把新内容合并进来继续打
                        requestRender(0L)
                    } else {
                        // 当前内容已展示完：做一次 schedule 确保图片/latex 最终态正确
                        requestSchedule(SCHEDULE_THROTTLE_MS)
                    }
                    stopTypewriterOnly() // renderRunnable will restart if needed
                    return
                }

                // 打字机核心：每 tick 展示多 1 个字符
                typedIndex = min(typedIndex + 1, total)
                val visible = renderedMarkdown!!.subSequence(0, typedIndex)
                markdownView.setText(visible)

                // When we update TextView manually (typewriter), Markwon plugins will not get
                // beforeSetText/afterSetText callbacks, so we must schedule ourselves.
                // Schedule as soon as a new AsyncDrawableSpan (images/latex) becomes visible,
                // but throttle scheduling for smoothness.
                if (visible is Spanned && (typedIndex == total || (typedIndex % SPAN_CHECK_EVERY_CHARS == 0))) {
                    // 这里扫描“已经可见”的图片/latex span 数量：一旦新增，就 schedule 触发加载/刷新
                    val count = visible.getSpans<io.noties.markwon.image.AsyncDrawableSpan?>(
                        0,
                        visible.length,
                        AsyncDrawableSpan::class.java
                    ).size
                    if (count > lastAsyncDrawableSpanCount) {
                        lastAsyncDrawableSpanCount = count
                        requestSchedule(SCHEDULE_THROTTLE_MS)
                    }
                }

                // Ensure final state is scheduled (in case of edge cases)
                if (typedIndex >= total) {
                    requestSchedule(SCHEDULE_THROTTLE_MS)
                }
                mainHandler.postDelayed(this, TYPE_DELAY_MS)
            }
        }
        mainHandler.postDelayed(typewriterRunnable!!, TYPE_DELAY_MS)
    }

    private fun requestSchedule(throttleMs: Long) {
        val now = SystemClock.uptimeMillis()
        val sinceLast = now - lastScheduleUptimeMs
        // 节流 schedule：避免每个字符都 schedule，影响流畅度
        val delay = max(0L, throttleMs - sinceLast)
        if (schedulePending) {
            return
        }
        schedulePending = true
        mainHandler.postDelayed(scheduleRunnable, delay)
    }

    private fun stopTypewriterOnly() {
        if (typewriterRunnable != null) {
            mainHandler.removeCallbacks(typewriterRunnable!!)
            typewriterRunnable = null
        }
    }

    private fun stopAll() {
        stopTypewriterOnly()
        if (schedulePending) {
            mainHandler.removeCallbacks(scheduleRunnable)
            schedulePending = false
        }
        if (renderPending) {
            mainHandler.removeCallbacks(renderRunnable)
            renderPending = false
        }
    }

    companion object {
        // timings
        private const val TYPE_DELAY_MS = 18L
        private const val RENDER_THROTTLE_MS = 60L
        private const val SCHEDULE_THROTTLE_MS = 80L
        private const val SPAN_CHECK_EVERY_CHARS = 6
    }
}


