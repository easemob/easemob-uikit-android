package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hyphenate.easeui.EaseIM.init
import com.hyphenate.easeui.R

class EaseChatReactionView @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyle: Int = 0,
): FrameLayout(context, attrs, defStyle) {

    init {
        initAttrs(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {

        context.obtainStyledAttributes(attrs, R.styleable.EaseChatReactionView).let { a ->
            a.getBoolean(R.styleable.EaseChatReactionView_ease_chat_item_sender, false).let {

            }
            a.recycle()
        }

    }


}