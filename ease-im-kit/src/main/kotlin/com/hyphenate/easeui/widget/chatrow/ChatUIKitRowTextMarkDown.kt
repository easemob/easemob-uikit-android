package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.ChatUIKitConstant
import com.hyphenate.easeui.common.extensions.addChildView
import com.hyphenate.easeui.common.extensions.containsChild
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.ChatUIKitAtMessageHelper
import com.hyphenate.easeui.common.helper.ChatUIKitDingMessageHelper
import com.hyphenate.easeui.model.ChatUIKitProfile
import com.hyphenate.easeui.model.ChatUIKitUser
import com.hyphenate.easeui.widget.StreamMarkdownChunkStore
import com.hyphenate.easeui.widget.StreamingMarkdownTypewriter
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale
import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeui.feature.chat.widgets.ChatUIKitMessageListLayout


open class ChatUIKitRowTextMarkDown @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0,
    isSender: Boolean
) : ChatUIKitRow(context, attrs, defStyleAttr, isSender) {
    protected val contentView: TextView? by lazy { findViewById(R.id.tv_chatcontent) }
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private var markwon: Markwon? = null
    private var typewriter: StreamingMarkdownTypewriter? = null
    private var boundMsgId: String? = null
    private var stickToBottom: Boolean = false
    private var lastAutoScrollUptimeMs: Long = 0L
    private val TAG = "ChatUIKitRowTextMarkDown"

    companion object{
        const val AT_PREFIX = "@"
        private const val AUTO_SCROLL_THROTTLE_MS = 60L
    }

    override fun onInflateView() {
        inflater.inflate(
            if (!isSender) R.layout.uikit_row_received_message else R.layout.uikit_row_sent_message,
            this
        )
    }

    override fun onSetUpView() {
        message?.run {
            (body as? ChatTextMessageBody)?.let {
                contentView?.let { view ->
                    val msgId = this.msgId

                    // RecyclerView 复用：当 Row 绑定到新消息时，清理旧的打字机状态
                    if (boundMsgId != msgId) {
                        typewriter?.destroy()
                        typewriter = null
                        boundMsgId = msgId
                    }

                    // 如果当前消息已在打字机中（例如 notifyItemChanged 导致 rebind），不要重置/重渲染，避免打断效果
                    if (typewriter == null) {
                        // 展示 markdown 内容：
                        // - 历史消息：直接全量渲染（无打字机）
                        // - 新流式消息第一片：如果先收到 chunk 且 item 尚未插入，会在这里 consume pending 后走打字机
                        val pending = StreamMarkdownChunkStore.consume(msgId)
                        if (!pending.isNullOrEmpty()) {
                            ChatLog.d(TAG, "onSetUpView: consume pending stream chunk, msgId=$msgId, len=${pending.length}")
                            appendData(pending)
                        } else {
                            renderMarkdownFull(it.message)
                        }
                    }

                    view.setOnLongClickListener { v ->
                        view.setTag(R.id.action_chat_long_click, true)
                        if (itemClickListener != null) {
                            itemClickListener!!.onBubbleLongClick(v, this)
                        } else false
                    }
                }

            }
        }
    }

    fun appendData(chunkData: String) {
        if (chunkData.isEmpty()) return
        val view = contentView ?: return
        val msgId = message?.msgId ?: ""

        view.movementMethod = LinkMovementMethod.getInstance()

        if (typewriter == null) {
            val mw = getMarkwon(view)
            // 只有当“开始流式打字机”的那一刻用户位于底部，才开启“粘住底部”滚动
            stickToBottom = (findRecyclerView(view)?.canScrollVertically(1) == false)
            typewriter = StreamingMarkdownTypewriter(mainHandler, mw, view) { _, _ ->
                maybeAutoScrollToBottom(view)
            }.also { it.reset() }
            ChatLog.d(TAG, "appendData: init typewriter, msgId=$msgId")
        }

        // 重要：StreamingMarkdownTypewriter.append 内部会自动切主线程、并且不会打断正在进行的打字机效果
        typewriter?.append(chunkData)
    }

    fun addChildToTopBubbleLayout(child: View?) {
        llTopBubble?.let {
            it.addChildView(child)
        }
    }

    /**
     * Add child view to bottom bubble layout.
     */
    fun addChildToBubbleBottomLayout(child: View?) {
        llBubbleBottom?.let {
            if (child != null && !it.containsChild(child)) {
                it.addView(child)
            }
        }
    }

    /**
     * Resolve long press event conflict with Relink
     * Refer to：https://www.jianshu.com/p/d3bef8449960
     */
    private fun replaceSpan() {
        (contentView?.text as? Spannable)?.let {
            val spans = it.getSpans(0, it.length, URLSpan::class.java)
            spans.forEach { item ->
                var url = item.url
                var index = it.toString().indexOf(url)
                var end = index + url.length
                if (index == -1) {
                    if (url.contains("http://")) {
                        url = url.replace("http://", "")
                    } else if (url.contains("https://")) {
                        url = url.replace("https://", "")
                    } else if (url.contains("rtsp://")) {
                        url = url.replace("rtsp://", "")
                    }
                    index = it.toString().indexOf(url)
                    end = index + url.length
                }
                if (index != -1) {
                    it.removeSpan(item)
                    it.setSpan(
                        AutolinkSpan(item.url), index, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }

            }
        }
    }

    override fun onMessageSuccess() {
        super.onMessageSuccess()

        message?.run {
            // Show "1 Read" if this msg is a ding-type msg.
            if (isSend() && ChatUIKitDingMessageHelper.get().isDingMessage(this)) {
                ackedView?.let {
                    it.visibility = VISIBLE
                    it.text = context.getString(R.string.uikit_group_ack_read_count, groupAckCount())
                }
            }
            // Set ack-user list change listener.
            ChatUIKitDingMessageHelper.get().setUserUpdateListener(this, object : ChatUIKitDingMessageHelper.IAckUserUpdateListener {
                override fun onUpdate(list: List<String>?) {
                    onAckUserUpdate(list?.size ?: 0)
                }
            })
        }
    }

    fun onAckUserUpdate(count: Int) {
        context.mainScope().launch {
            if (isSender) {
                ackedView?.visibility = VISIBLE
                ackedView?.text = String.format(context.getString(R.string.uikit_group_ack_read_count), count)
            }
        }
    }

    private fun replacePickAtSpan() {
        val message = this.message
        message?.ext()?.let {
            if (it.containsKey(ChatUIKitConstant.MESSAGE_ATTR_AT_MSG)) {
                var atAll = ""
                var atMe = ""
                var start = 0
                var end = 0
                val isAtMe: Boolean = ChatUIKitAtMessageHelper.get().isAtMeMsg(message)
                if (isAtMe) {
                    var currentUserGroupInfo: ChatUIKitUser? = ChatUIKitProfile.getGroupMember(message.conversationId(), ChatClient.getInstance().currentUser)?.toUser()
                    try {
                        val jsonArray: JSONArray =
                            message.getJSONArrayAttribute(ChatUIKitConstant.MESSAGE_ATTR_AT_MSG)
                        for (i in 0 until jsonArray.length()) {
                            val atId = jsonArray[i]
                            if (atId == ChatClient.getInstance().currentUser){
                                currentUserGroupInfo?.let { user->
                                    if (contentView?.text.toString()
                                            .contains(user.userId)
                                    ) {
                                        atMe = user.userId
                                    } else if (contentView?.text.toString()
                                            .contains(user.getRemarkOrName().toString())
                                    ) {
                                        atMe = user.getRemarkOrName().toString()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        val atUsername: String =
                            message.getStringAttribute(ChatUIKitConstant.MESSAGE_ATTR_AT_MSG, null)
                        val s = atUsername.uppercase(Locale.getDefault())
                        if (s == ChatUIKitConstant.MESSAGE_ATTR_VALUE_AT_MSG_ALL.uppercase()) {
                            atAll = atUsername.substring(0, 1)
                                .uppercase(Locale.getDefault()) + atUsername.substring(1).lowercase(
                                Locale.getDefault()
                            )
                        }
                    }
                }
                if (!TextUtils.isEmpty(atMe)) {
                    atMe = AT_PREFIX + atMe
                    start = contentView?.text.toString().indexOf(atMe)
                    end = start + atMe.length
                }
                if (!TextUtils.isEmpty(atAll)) {
                    atAll = AT_PREFIX + atAll
                    start = contentView?.text.toString().indexOf(atAll)
                    end = start + atAll.length
                }
                if (isAtMe) {
                    if (start != -1 && end > 0 && message.direct() === ChatMessageDirection.RECEIVE) {
                        val spannableString = SpannableString(contentView?.text)
                        spannableString.setSpan(
                            ForegroundColorSpan(context.resources.getColor(R.color.ease_chat_mention_text_color)),
                            start,
                            end,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        contentView?.text = spannableString
                    }
                }
            }
        }

    }

    val getBubbleBottom : LinearLayout? = llBubbleBottom

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // RecyclerView item 回收时释放资源，避免 Handler 持有 View 导致泄漏
        typewriter?.destroy()
        typewriter = null
        stickToBottom = false
    }

    private fun getMarkwon(view: TextView): Markwon {
        val existed = markwon
        if (existed != null) return existed
        val created = Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            // ext-latex 单行 $$...$$ 需要 inline parser
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(view.textSize) { builder -> builder.inlinesEnabled(true) })
            .usePlugin(TablePlugin.create(context))
            .build()
        markwon = created
        return created
    }

    private fun renderMarkdownFull(markdown: String?) {
        val view = contentView ?: return
        val text = markdown ?: ""
        view.movementMethod = LinkMovementMethod.getInstance()
        try {
            // 历史消息：直接渲染完整 markdown（不打字机）
            getMarkwon(view).setMarkdown(view, text)
            replaceSpan()
            replacePickAtSpan()
        } catch (t: Throwable) {
            ChatLog.e(TAG, "renderMarkdownFull failed: ${t.message}")
            view.text = text
        }
    }

    private fun maybeAutoScrollToBottom(anchorView: View) {
        val rv = findRecyclerView(anchorView) ?: return

        // 如果用户手动滚回到底部，可以恢复“粘底”
        if (!stickToBottom && rv.scrollState == RecyclerView.SCROLL_STATE_IDLE && !rv.canScrollVertically(1)) {
            stickToBottom = true
            ChatLog.d(TAG, "autoScroll: resume stickToBottom=true")
        }

        if (!stickToBottom) return
        // 用户正在拖动/惯性滚动时不抢手势
        if (rv.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
            stickToBottom = false
            ChatLog.d(TAG, "autoScroll: stop stickToBottom=false (scrollState=${rv.scrollState})")
            return
        }
        // 如果上层 MessageListLayout 判定不允许自动滚到底（例如用户已上滑离开底部），立刻停止
        val listLayout = findMessageListLayout(anchorView)
        if (listLayout != null && !listLayout.isCanAutoScrollToBottom) {
            stickToBottom = false
            ChatLog.d(TAG, "autoScroll: stop stickToBottom=false (listLayout disallow)")
            return
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastAutoScrollUptimeMs < AUTO_SCROLL_THROTTLE_MS) return
        lastAutoScrollUptimeMs = now

        // 关键：不要用 scrollToPosition(last)（会把最后一个 item 放到屏幕顶部，造成“总是看见顶部 + 闪烁”）
        // 我们只做“向下补齐 delta”，让底部保持贴住（更稳定，不跳动）
        if (!rv.canScrollVertically(1)) return
        val childCount = rv.childCount
        if (childCount <= 0) return
        val lastChild = rv.getChildAt(childCount - 1) ?: return
        val dy = lastChild.bottom - rv.height
        if (dy > 0) {
            rv.scrollBy(0, dy)
            if (dy > 20) {
                ChatLog.d(TAG, "autoScroll: scrollBy dy=$dy")
            }
        }
    }

    private fun findRecyclerView(view: View): RecyclerView? {
        var p: ViewParent? = view.parent
        while (p != null) {
            if (p is RecyclerView) return p
            p = p.parent
        }
        return null
    }

    private fun findMessageListLayout(view: View): ChatUIKitMessageListLayout? {
        var p: ViewParent? = view.parent
        while (p != null) {
            if (p is ChatUIKitMessageListLayout) return p
            p = p.parent
        }
        return null
    }

}