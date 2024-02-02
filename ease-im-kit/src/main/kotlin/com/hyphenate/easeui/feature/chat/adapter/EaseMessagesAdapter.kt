package com.hyphenate.easeui.feature.chat.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.feature.chat.config.EaseChatMessageItemConfig
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListItemClickListener
import com.hyphenate.easeui.feature.chat.viewholders.EaseChatRowViewHolder
import com.hyphenate.easeui.feature.chat.viewholders.EaseChatViewHolderFactory
import com.hyphenate.easeui.feature.chat.viewholders.EaseMessageViewType
import com.hyphenate.easeui.feature.chat.reply.interfaces.OnMessageReplyViewClickListener
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.widget.chatrow.EaseChatRow
import com.hyphenate.easeui.widget.chatrow.EaseChatRowText

open class EaseMessagesAdapter(
    private var messageItemConfig: EaseChatMessageItemConfig? = null
): EaseBaseRecyclerViewAdapter<EaseMessage>() {
    private var itemClickListener: OnMessageListItemClickListener? = null
    private var replyViewClickListener: OnMessageReplyViewClickListener? = null
    private var highlightPosition = -1
    private var colorAnimation: ValueAnimator? = null

    override fun getItemNotEmptyViewType(position: Int): Int {
        return EaseChatViewHolderFactory.getViewType(getItem(position)?.getMessage())
    }

    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<EaseMessage> {
        return EaseChatViewHolderFactory.createViewHolder(parent, EaseMessageViewType.from(viewType))
    }

    override fun onBindViewHolder(holder: ViewHolder<EaseMessage>, position: Int) {
        getItem(position)?.run {
            setConfig(messageItemConfig)
        }
        super.onBindViewHolder(holder, position)

        if (holder is EaseChatRowViewHolder && holder.itemView is EaseChatRow) {
            (holder.itemView as? EaseChatRow)?.let {
                it.setOnMessageListItemClickListener(itemClickListener)
            }
            (holder.itemView as? EaseChatRowText)?.let {
                it.setOnMessageReplyViewClickListener(replyViewClickListener)
            }
        }

        if (position == highlightPosition) {
            val outLayout: View = holder.itemView.findViewById(R.id.cl_bubble_out)
            outLayout?.let { startAnimator(it) } ?: startAnimator(holder.itemView)
            highlightPosition = -1
        }
    }

    /**
     * Set message item config.
     */
    fun setItemConfig(config: EaseChatMessageItemConfig?) {
        this.messageItemConfig = config
    }

    /**
     * Set message item click listener.
     */
    fun setOnMessageListItemClickListener(listener: OnMessageListItemClickListener?) {
        itemClickListener = listener
    }

    /**
     * Highlight the item view.
     * @param position
     */
    fun highlightItem(position: Int) {
        highlightPosition = position
        notifyItemChanged(position)
    }

    private fun startAnimator(view: View) {
        val background = view.background
        val darkColor = ContextCompat.getColor(mContext!!, R.color.ease_chat_item_bg_dark)
        colorAnimation?.cancel()
        colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), Color.TRANSPARENT, darkColor)
        colorAnimation?.duration = 500
        colorAnimation?.addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
            if (animator.animatedValue as Int == darkColor) {
                view.background = background
            } else if (animator.animatedValue as Int == 0) {
                view.background = null
            }
        }
        colorAnimation?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                view.background = background
            }
        })
        colorAnimation?.start()
    }

    fun setOnMessageReplyViewClickListener(listener: OnMessageReplyViewClickListener?) {
        this.replyViewClickListener = listener
    }
}