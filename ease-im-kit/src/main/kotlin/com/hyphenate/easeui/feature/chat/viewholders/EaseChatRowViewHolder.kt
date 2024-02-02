package com.hyphenate.easeui.feature.chat.viewholders

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.feature.chat.interfaces.OnItemBubbleClickListener
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.widget.chatrow.EaseChatRow

open class EaseChatRowViewHolder(itemView: View): EaseBaseRecyclerViewAdapter.ViewHolder<EaseMessage>(itemView),
    OnItemBubbleClickListener {
    private val TAG = EaseChatRowViewHolder::class.java.simpleName
    protected var mContext: Context = itemView.context
    private var chatRow: EaseChatRow? = null
    private var message: EaseMessage? = null

    init {
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        itemView.layoutParams = params
    }

    override fun initView(itemView: View?) {
        chatRow = itemView as EaseChatRow
        chatRow?.setOnItemBubbleClickListener(this)
    }

    override fun setData(item: EaseMessage?, position: Int) {
        message = item
        chatRow?.setUpView(item, position)
        handleMessage()
    }

    override fun setDataList(data: List<EaseMessage>?, position: Int) {
        super.setDataList(data, position)
        chatRow?.setTimestamp(if (position == 0) null else data!![position - 1])
    }

    override fun onBubbleClick(message: EaseMessage?) {

    }

    open fun onDetachedFromWindow() {}

    open fun handleMessage() {
        message?.getMessage()?.run {
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
    protected open fun handleSendMessage(message: EaseMessage?) {
        // Update the view according to the message current status.
        //getChatRow().updateView(message)
    }

    /**
     * receive message
     * @param message
     */
    protected open fun handleReceiveMessage(message: EaseMessage?) {}

    open fun getContext(): Context {
        return mContext
    }

    open fun getChatRow(): EaseChatRow? {
        return chatRow
    }
}