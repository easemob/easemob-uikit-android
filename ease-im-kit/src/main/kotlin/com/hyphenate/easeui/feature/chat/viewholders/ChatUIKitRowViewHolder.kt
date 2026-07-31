package com.hyphenate.easeui.feature.chat.viewholders

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.base.ChatUIKitBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatCallback
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.feature.chat.interfaces.OnItemBubbleClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageReadReceiptSendCallback
import com.hyphenate.easeui.widget.chatrow.ChatUIKitRow
import kotlinx.coroutines.launch

open class ChatUIKitRowViewHolder(itemView: View): ChatUIKitBaseRecyclerViewAdapter.ViewHolder<ChatMessage>(itemView),
    OnItemBubbleClickListener {
    private var messageReadReceiptSendCallback: OnMessageReadReceiptSendCallback? = null
    private val TAG = ChatUIKitRowViewHolder::class.java.simpleName
    protected var mContext: Context = itemView.context
    private var chatRow: ChatUIKitRow? = null
    private var message: ChatMessage? = null

    init {
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        itemView.layoutParams = params
    }

    override fun initView(itemView: View?) {
        chatRow = itemView as ChatUIKitRow
        chatRow?.setOnItemBubbleClickListener(this)
    }

    override fun setData(item: ChatMessage?, position: Int) {
        message = item
        chatRow?.setUpView(item, position)
        handleMessage()
    }

    override fun setDataList(data: List<ChatMessage>?, position: Int) {
        super.setDataList(data, position)
        data?.let {
            if (position < data.size) {
                chatRow?.setTimestamp(if (position == 0) null else data[position - 1])
            }
        }
    }

    override fun onBubbleClick(message: ChatMessage?) {

    }

    open fun onDetachedFromWindow() {}

    open fun handleMessage() {
        message?.run {
            if (isSend()) {
                handleSendMessage(message)
            } else {
                handleReceiveMessage(message)
            }
        }
    }

    /**
     * send message
     * @param message
     */
    protected open fun handleSendMessage(message: ChatMessage?) {
        // Update the view according to the message current status.
        //getChatRow().updateView(message)
    }

    /**
     * receive message
     * @param message
     */
    protected open fun handleReceiveMessage(message: ChatMessage?) {
        //Here no longer send read_ack message separately, instead enter the chat page to send channel_ack
        //New messages are sent in the onReceiveMessage method of the chat page, except for video
        // , voice and file messages, and send read_ack messages
        if (ChatUIKitClient.getConfig()?.chatConfig?.enableSendChannelAck == true && ChatUIKitClient.getConfig()?.chatConfig?.showUnreadNotificationInChat == false) {
            return
        }
        message?.let { msg ->
            // send message read receipt
            val type = msg.type
            //Video, voice and files need to be clicked before sending
            if (type === ChatMessageType.VIDEO || type === ChatMessageType.VOICE || type === ChatMessageType.FILE) {
                return
            }
            if (!msg.isPeerRead && msg.chatType === ChatType.Chat) {
                ChatClient.getInstance().chatManager()
                    .asyncSendMessageReadReceipts(listOf(msg), object : ChatCallback {
                        override fun onSuccess() {
                            getContext().mainScope().launch {
                                messageReadReceiptSendCallback?.onSendReadReceiptSuccess(msg)
                            }
                        }

                        override fun onError(code: Int, errorMsg: String?) {
                            getContext().mainScope().launch {
                                messageReadReceiptSendCallback?.onSendReadReceiptError(msg, code, errorMsg)
                            }
                        }
                    })
            }
        }
    }

    open fun getContext(): Context {
        return mContext
    }

    open fun getChatRow(): ChatUIKitRow? {
        return chatRow
    }

    /**
     * Set message ack send callback.
     */
    fun setOnMessageReadReceiptSendCallback(callback: OnMessageReadReceiptSendCallback?) {
        this.messageReadReceiptSendCallback = callback
    }
}