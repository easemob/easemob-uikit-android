package com.hyphenate.easeui.feature.chat.chathistory.widget

import android.content.Context
import android.util.AttributeSet
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.extensions.getDateFormat
import com.hyphenate.easeui.widget.chatrow.ChatUIKitRowBigExpression

/**
 * big emoji icons
 *
 */
open class ChatUIKitRowHistoryBigExpression @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0,
    isSender: Boolean = false
) : ChatUIKitRowBigExpression(context, attrs, defStyleAttr, isSender) {

    override fun onInflateView() {
        inflater.inflate(R.layout.uikit_row_history_bigexpression, this)
    }

    override fun setOtherTimestamp(preMessage: ChatMessage?) {
        timeStampView?.let {
            preMessage?.let { msg ->
                it.text = msg.getDateFormat(true)
                it.visibility = VISIBLE
            }
        }
    }

    override fun onSetUpView() {
        super.onSetUpView()
        usernickView?.let {
            if (!it.text.toString().trim().isNullOrEmpty()) {
                it.visibility = VISIBLE
            }
        }
    }
}
