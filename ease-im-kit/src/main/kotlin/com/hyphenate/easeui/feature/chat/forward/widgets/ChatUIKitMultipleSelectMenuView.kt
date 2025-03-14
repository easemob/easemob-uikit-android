package com.hyphenate.easeui.feature.chat.forward.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatUIKitConstant
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.extensions.plus
import com.hyphenate.easeui.databinding.UikitLayoutChatMessagesMultiSelectMenuBinding
import com.hyphenate.easeui.feature.chat.forward.helper.ChatUIKitMessageMultiSelectHelper
import com.hyphenate.easeui.feature.chat.interfaces.IChatTopExtendMenu
import com.hyphenate.easeui.feature.chat.interfaces.OnMultipleSelectChangeListener
import com.hyphenate.easeui.interfaces.OnMenuDismissListener
import com.hyphenate.easeui.interfaces.OnMultiSelectMenuListener
import com.hyphenate.easeui.model.ChatUIKitEvent

@SuppressLint("ViewConstructor")
class ChatUIKitMultipleSelectMenuView @JvmOverloads constructor(
    private val conversationId: String,
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), IChatTopExtendMenu {

    private var dismissListener: OnMenuDismissListener? = null
    private val binding: UikitLayoutChatMessagesMultiSelectMenuBinding by lazy {
        UikitLayoutChatMessagesMultiSelectMenuBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

    private var listener: OnMultiSelectMenuListener? = null

    private var message: ChatMessage? = null

    init {
        initListener()
    }

    /**
     * Add the message to the selected list.
     * Call it before the view was attached to extend top view.
     */
    fun setSelectedMessage(message: ChatMessage?) {
        this.message = message
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitConstant.EASE_MULTIPLE_SELECT).post(context.mainScope(),
            ChatUIKitEvent(ChatUIKitEvent.EVENT.ADD.name, ChatUIKitEvent.TYPE.NOTIFY, context + conversationId))
        if (ChatUIKitClient.getConfig()?.chatConfig?.enableSendCombineMessage == false) {
            binding.ivMultiSelectForward.visibility = INVISIBLE
        } else {
            binding.ivMultiSelectForward.visibility = VISIBLE
        }
        ChatUIKitMessageMultiSelectHelper.getInstance().init(context, conversationId)
        ChatUIKitMessageMultiSelectHelper.getInstance().setMultiStyle(context, conversationId, true)
        message?.let {
            ChatUIKitMessageMultiSelectHelper.getInstance().addChatMessage(context, it)
        }
    }

    private fun initListener() {
        binding.ivMultiSelectDelete.setOnClickListener {
            listener?.onDeleteClick(ChatUIKitMessageMultiSelectHelper.getInstance().getSortedMessages(context, conversationId) ?: listOf())
        }

        binding.ivMultiSelectForward.setOnClickListener {
            listener?.onForwardClick(ChatUIKitMessageMultiSelectHelper.getInstance().getSortedMessages(context, conversationId) ?: listOf())
        }
        ChatUIKitMessageMultiSelectHelper.getInstance().setOnMultipleSelectDataChangeListener(context, conversationId, object :
            OnMultipleSelectChangeListener {

            override fun onMultipleSelectDataChange(key: String) {
                if (key == context + conversationId) {
                    ChatUIKitMessageMultiSelectHelper.getInstance().isEmpty(context, conversationId)?.let {
                        binding.ivMultiSelectDelete.isEnabled = !it
                        binding.ivMultiSelectForward.isEnabled = !it
                    }
                }
            }

            override fun onMultipleSelectModelChange(key: String, isMultiStyle: Boolean) {
                if (key == context + conversationId && !isMultiStyle) {
                    ChatUIKitMessageMultiSelectHelper.getInstance().clear(context, conversationId)
                    showTopExtendMenu(false)
                    dismissListener?.onDismiss()
                }
            }


        })
    }

    override fun showTopExtendMenu(isShow: Boolean) {
        visibility = if (isShow) {
            VISIBLE
        } else {
            GONE
        }
    }

    fun setOnMultiSelectMenuListener(listener: OnMultiSelectMenuListener) {
        this.listener = listener
    }

    fun setOnMenuDismissListener(listener: OnMenuDismissListener) {
        this.dismissListener = listener
    }
}