package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.util.AttributeSet
import com.hyphenate.chat.EMTextMessageBody
import com.hyphenate.easeui.R
import com.hyphenate.util.EMLog
import io.noties.markwon.Markwon

/**
 * Stream message chat row.
 * Used to display stream messages with Markdown rendering.
 * Inherits from EaseChatRowText to reuse existing functionality like reply, translation, URL preview, etc.
 */
open class EaseChatRowStream @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    isSender: Boolean
) : EaseChatRowText(context, attrs, defStyleAttr, isSender) {

    private var markwon: Markwon? = null

    init {
        // Initialize Markwon instance
        markwon = Markwon.create(context)
    }

    override fun onSetUpView() {
        message?.run {
            val streamChunk = getStreamChunk()
            if (streamChunk != null) {
                // Get text content from stream chunk
                val textContent = (body as EMTextMessageBody).message
                val textType = streamChunk.customType
                
                contentView?.let { view ->
                    // Check if it's markdown format
                    if (textType == "markdown" && !textContent.isNullOrEmpty()) {
                        EMLog.e("stream", "display stream message: content: "+textContent)
                        // Render markdown using Markwon
                        markwon?.setMarkdown(view, textContent)
                    } else {
                        // Fallback to plain text
                        view.text = textContent
                    }
                    
                    // Set long click listener (reuse parent functionality)
                    view.setOnLongClickListener { v ->
                        view.setTag(R.id.action_chat_long_click, true)
                        if (itemClickListener != null) {
                            itemClickListener!!.onBubbleLongClick(v, this)
                        } else false
                    }
                }
                
                // Note: We don't call replaceSpan() and replacePickAtSpan() here
                // because Markwon handles markdown rendering, and these methods are
                // designed for plain text with URL spans and @ mentions.
                // If needed, we can add similar functionality for markdown content later.
            }
        }
    }
}

