package com.hyphenate.easeui.feature.chat.reply

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import coil.load
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.databinding.EaseWidgetChatMessageReplyBinding
import com.hyphenate.easeui.feature.chat.interfaces.IChatTopExtendMenu
import com.hyphenate.easeui.feature.chat.reply.interfaces.IChatMessageReply
import com.hyphenate.easeui.feature.chat.reply.interfaces.IChatMessageReplyResultView
import com.hyphenate.easeui.viewmodel.reply.EaseChatMessageReplyViewModel

class EaseChatExtendMessageReplyView(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr),
    IChatMessageReply, IChatTopExtendMenu,
    IChatMessageReplyResultView {

    private val binding: EaseWidgetChatMessageReplyBinding by lazy {
        EaseWidgetChatMessageReplyBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
    private var viewModel: EaseChatMessageReplyViewModel? = null

    init {
        binding.cancelSelect.setOnClickListener { hideQuoteView() }
        viewModel = EaseChatMessageReplyViewModel()
        viewModel?.attachView(this)
    }
    override fun showTopExtendMenu(isShow: Boolean) {
        this.visibility = if (isShow) VISIBLE else GONE
    }

    override fun startQuote(message: ChatMessage?) {
        hideQuoteView()
        viewModel?.showQuoteMessageInfo(message)
    }

    override fun hideQuoteView() {
        this.visibility = GONE
        binding.quoteName.text = ""
        binding.quoteContent.text = ""
        binding.quoteIcon.visibility = GONE
        binding.quoteImage.visibility = GONE
        binding.quoteVideoIcon.visibility = GONE
    }

    override fun setViewModel(viewModel: EaseChatMessageReplyViewModel?) {
        this.viewModel = viewModel
        this.viewModel?.attachView(this)
    }

    override fun showQuoteMessageNickname(nickname: String?) {
        binding.quoteName.text = nickname
    }

    override fun showQuoteMessageContent(content: StringBuilder?) {
        binding.quoteContent.text = content
        showTopExtendMenu(true)
    }

    override fun showQuoteMessageAttachment(
        type: ChatMessageType?,
        localPath: String?,
        remotePath: String?,
        defaultResource: Int
    ) {
        binding.quoteImage.visibility = GONE
        binding.quoteVideoIcon.visibility = GONE
        if (type == ChatMessageType.IMAGE || type == ChatMessageType.VIDEO) {
            binding.quoteImage.load(if (localPath.isNullOrEmpty()) remotePath else localPath) {
                placeholder(defaultResource)
                error(defaultResource)
                listener(onSuccess = { _, _ ->
                    if (type == ChatMessageType.VIDEO) {
                        binding.quoteVideoIcon.visibility = VISIBLE
                    }
                })
            }
            binding.quoteImage.visibility = VISIBLE
        }

        binding.quoteIcon.visibility = VISIBLE
        when(type) {
            ChatMessageType.IMAGE -> {
                binding.quoteIcon.setImageResource(R.drawable.ease_chat_quote_icon_image)
            }
            ChatMessageType.VIDEO -> {
                binding.quoteIcon.setImageResource(R.drawable.ease_chat_quote_icon_video)
            }
            ChatMessageType.VOICE ->{
                binding.quoteIcon.setImageResource(R.drawable.ease_chat_quote_icon_voice)
            }
            ChatMessageType.FILE ->{
                binding.quoteIcon.setImageResource(R.drawable.ease_chat_quote_icon_file)
            }
            ChatMessageType.CUSTOM ->{
                binding.quoteIcon.setImageResource(R.drawable.ease_chat_quote_icon_user_card)
            }
            else -> {
                binding.quoteIcon.visibility = GONE
            }
        }
    }

    override fun onShowError(code: Int, message: String?) {

    }
}