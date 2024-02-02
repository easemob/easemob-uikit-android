package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatCombineMessageBody

class EaseChatRowCombine @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0,
    isSender: Boolean = false
) : EaseChatRow(context, attrs, defStyleAttr, isSender) {
    private val contentView: TextView? by lazy { findViewById(R.id.tv_chatcontent) }
    private val tvChatSummary: TextView? by lazy { findViewById(R.id.tv_chat_summary) }

    override fun onInflateView() {
        inflater.inflate(
            if (!isSender) R.layout.ease_row_received_combine else R.layout.ease_row_sent_combine,
            this
        )
    }

    override fun onSetUpView() {
        message?.getMessage()?.run {
            (body as? ChatCombineMessageBody)?.let {
                contentView?.text = it.title
                tvChatSummary?.let { view ->
                    if (!TextUtils.isEmpty(it.summary)) {
                        view.text = it.summary
                        view.visibility = View.VISIBLE
                    } else {
                        view.visibility = View.GONE
                    }
                }
            }
        }
    }
}