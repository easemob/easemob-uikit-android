package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.extensions.getEmojiText
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.EaseAtMessageHelper
import com.hyphenate.easeui.feature.chat.reply.EaseChatMessageReplyView
import com.hyphenate.easeui.feature.chat.reply.interfaces.OnMessageReplyViewClickListener
import com.hyphenate.easeui.common.helper.EaseDingMessageHelper
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.provider.getSyncMemberProfile
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

open class EaseChatRowText @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0,
    isSender: Boolean
) : EaseChatRow(context, attrs, defStyleAttr, isSender) {
    private var replyViewClickListener: OnMessageReplyViewClickListener? = null
    protected val contentView: TextView? by lazy { findViewById(R.id.tv_chatcontent) }
    private val quoteView: EaseChatMessageReplyView? by lazy { findViewById(R.id.chat_quote_view) }

    companion object{
        const val AT_PREFIX = "@"
    }

    override fun onInflateView() {
        inflater.inflate(
            if (!isSender) R.layout.ease_row_received_message else R.layout.ease_row_sent_message,
            this
        )
    }

    override fun onFindViewById() {
        resetBackground()
    }

    override fun onSetUpView() {
        message?.getMessage()?.run {
            (body as? ChatTextMessageBody)?.let {
                contentView?.let { view ->
                    view.text = it.message.getEmojiText(context)
                    view.setOnLongClickListener { v ->
                        view.setTag(R.id.action_chat_long_click, true)
                        if (itemClickListener != null) {
                            itemClickListener!!.onBubbleLongClick(v, this)
                        } else false
                    }
                }
                replaceSpan()
                replacePickAtSpan()
            }
        }
        resetBackground()
        onSetUpReplyView(message)
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

        message?.getMessage()?.run {
            // Show "1 Read" if this msg is a ding-type msg.
            if (isSend() && EaseDingMessageHelper.get().isDingMessage(this)) {
                ackedView?.let {
                    it.visibility = View.VISIBLE
                    it.text = context.getString(R.string.ease_group_ack_read_count, groupAckCount())
                }
            }
            // Set ack-user list change listener.
            EaseDingMessageHelper.get().setUserUpdateListener(this, object : EaseDingMessageHelper.IAckUserUpdateListener {
                override fun onUpdate(list: List<String>?) {
                    onAckUserUpdate(list?.size ?: 0)
                }
            })
        }
    }

    fun onSetUpReplyView(message: EaseMessage?) {
        if (quoteView == null) {
            ChatLog.e(TAG, "view is null, don't setup quote view")
            return
        }
        quoteView?.visibility = GONE
        quoteView?.setOnMessageReplyViewClickListener(replyViewClickListener)
        val isUpdated: Boolean = quoteView!!.updateMessageInfo(message?.getMessage())
        if (isUpdated) {
            quoteView?.visibility = VISIBLE
            updateBackground()
        }
    }

    fun resetBackground() {

    }

    fun updateBackground() {

    }

    fun onAckUserUpdate(count: Int) {
        context.mainScope().launch {
            if (isSender) {
                ackedView?.visibility = View.VISIBLE
                ackedView?.text = String.format(context.getString(R.string.ease_group_ack_read_count), count)
            }
        }
    }

    fun setOnMessageReplyViewClickListener(replyViewClickListener: OnMessageReplyViewClickListener?) {
        this.replyViewClickListener = replyViewClickListener
        if (quoteView != null) {
            quoteView?.setOnMessageReplyViewClickListener(replyViewClickListener)
        }
    }

    private fun replacePickAtSpan() {
        val message = this.message?.getMessage()
        message?.ext()?.let {
            if (it.containsKey(EaseConstant.MESSAGE_ATTR_AT_MSG)) {
                var atAll = ""
                var atMe = ""
                var start = 0
                var end = 0
                val isAtMe: Boolean = EaseAtMessageHelper.get().isAtMeMsg(message)
                if (isAtMe) {
                    var currentUserGroupInfo: EaseUser? = null
                    EaseIM.getGroupMemberProfileProvider()?.getSyncMemberProfile(
                        message.conversationId(),
                        ChatClient.getInstance().currentUser,
                    )?.let { profile ->
                        currentUserGroupInfo = profile.toUser()
                    }
                    try {
                        val jsonArray: JSONArray =
                            message.getJSONArrayAttribute(EaseConstant.MESSAGE_ATTR_AT_MSG)
                        for (i in 0 until jsonArray.length()) {
                            val atId = jsonArray[i]
                            if (atId == ChatClient.getInstance().currentUser){
                                currentUserGroupInfo?.let { user->
                                    if (contentView?.text.toString()
                                            .contains(user.userId)
                                    ) {
                                        atMe = user.userId
                                    } else if (contentView?.text.toString()
                                            .contains(user.nickname.toString())
                                    ) {
                                        atMe = user.nickname.toString()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        val atUsername: String =
                            message.getStringAttribute(EaseConstant.MESSAGE_ATTR_AT_MSG, null)
                        val s = atUsername.uppercase(Locale.getDefault())
                        if (s == EaseConstant.MESSAGE_ATTR_VALUE_AT_MSG_ALL.uppercase()) {
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
}