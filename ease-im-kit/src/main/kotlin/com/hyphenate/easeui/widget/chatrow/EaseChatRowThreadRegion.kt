package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class EaseChatRowThreadRegion @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyle: Int = 0,
): FrameLayout(context, attrs, defStyle) {

    init {
        initAttrs(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {

    }
}