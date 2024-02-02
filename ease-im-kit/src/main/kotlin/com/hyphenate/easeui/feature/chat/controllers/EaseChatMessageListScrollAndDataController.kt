package com.hyphenate.easeui.feature.chat.controllers

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.feature.chat.enums.EaseLoadDataType
import com.hyphenate.easeui.feature.chat.adapter.EaseMessagesAdapter
import com.hyphenate.easeui.model.EaseMessage
import kotlinx.coroutines.launch

/**
 * The controller to control the scroll and data of the message list.
 */
class EaseChatMessageListScrollAndDataController(
    private val rvList: RecyclerView,
    private val adapter: EaseMessagesAdapter,
    private val context: Context
) {

    /**
     * Whether to scroll to the bottom when the message list changes.
     */
    private var isNeedScrollToBottomWhenChange = true
    private var loadDataType: EaseLoadDataType = EaseLoadDataType.LOCAL
    private var recyclerViewLastHeight = 0
    private var targetScrollMsgId: String? = null

    init {
        rvList.viewTreeObserver.addOnGlobalLayoutListener {
            val height = rvList.height
            if (recyclerViewLastHeight == 0) recyclerViewLastHeight = height
            if (recyclerViewLastHeight != height) {
                if (!adapter.mData.isNullOrEmpty()
                    && rvList.canScrollVertically(1)
                    && loadDataType != EaseLoadDataType.HISTORY
                    && isNeedScrollToBottomWhenChange) {
                    scrollToBottom()
                }
            }
            recyclerViewLastHeight = height
        }
    }

    fun setLoadDataType(loadDataType: EaseLoadDataType) {
        this.loadDataType = loadDataType
    }

    /**
     * Scroll to the bottom of the list.
     */
    fun scrollToBottom() {
        context.mainScope().launch {
            scrollToPosition(adapter.itemCount - 1)
        }
    }

    /**
     * Scroll to the target position.
     */
    fun scrollToPosition(position: Int) {
        if (position < 0 || position >= adapter.itemCount) return
        rvList.post {
            if (isLastPosition(position) && !rvList.canScrollVertically(1)) return@post
            val manager = rvList.layoutManager
            if (manager is LinearLayoutManager) {
                manager.scrollToPositionWithOffset(position, 0)
                checkIfMoveToBottom(position, manager)
            }
        }
    }

    /**
     * Smooth scroll to the target position.
     */
    fun smoothScrollToPosition(position: Int, isMoveToTop: Boolean = true) {
        if (position < 0 || position >= adapter.itemCount) return
        val manager = rvList.layoutManager
        if (manager !is LinearLayoutManager) return
        rvList.post {
            val moveHeight = if (isMoveToTop) -10 else 10
            rvList.smoothScrollBy(0, moveHeight, null, 10)
            rvList.postDelayed({
                val itemViewHeight: Int = getViewHeight(position, manager)
                if (itemViewHeight != -1) {
                    val excessHeight =
                        if (isMoveToTop) -itemViewHeight + 10 else itemViewHeight - 10
                    rvList.smoothScrollBy(0, excessHeight, null, 900)
                } else {
                    manager.scrollToPositionWithOffset(
                        position,
                        0
                    )
                }
            }, 100)
        }
    }

    private fun isLastPosition(position: Int): Boolean {
        return position == adapter.itemCount - 1
    }

    private fun checkIfMoveToBottom(position: Int, layoutManager: LinearLayoutManager) {
        if (position < 0 || position >= adapter.itemCount) return
        if (!rvList.canScrollVertically(1)) return
        if (!isLastPosition(position) || !isFullScreen()) return
        rvList.post {
            val rvPosition: Int =
                layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition()
            if (rvList.childCount > rvPosition) {
                val bottom = rvList.getChildAt(rvPosition).bottom
                val height = rvList.height
                layoutManager.scrollToPositionWithOffset(position, height - bottom)
            }
        }
    }

    private fun isFullScreen(): Boolean {
        var isOverOneScreen = false
        var totalHeight = 0
        for (i in 0 until rvList.childCount) {
            totalHeight += rvList.getChildAt(i).height
            if (rvList.height < totalHeight) {
                isOverOneScreen = true
                break
            }
        }
        return isOverOneScreen
    }

    private fun getViewHeight(position: Int, layoutManager: LinearLayoutManager): Int {
        val view = layoutManager.findViewByPosition(position)
        var height = -1
        if (view != null) {
            height = view.measuredHeight
        } else {
            val holder = rvList.findViewHolderForAdapterPosition(position)
            if (holder != null) {
                height = holder.itemView.height
            }
        }
        return height
    }

    fun refreshMessages(messages: List<EaseMessage>) {
        context.mainScope().launch {
            adapter.setData(messages.toMutableList())
        }
    }

    fun refreshMessage(message: EaseMessage?) {
        if (message == null) return
        context.mainScope().launch {
            val position = adapter.data?.indexOfLast { it.getMessage().msgId == message.getMessage().msgId } ?: -1
            if (position != -1) {
                adapter.notifyItemChanged(position, 0)
            }
        }
    }

    fun removeMessage(message: EaseMessage?) {
        if (message == null) return
        context.mainScope().launch {
            var position = -1
            adapter.mData?.forEachIndexed { index, easeMessage ->
                if (easeMessage.getMessage().msgId == message.getMessage().msgId) {
                    position = index
                    return@forEachIndexed
                }
            }
            if (position != -1) {
                adapter.mData?.removeAt(position)
                adapter.notifyDataSetChanged()
            }
        }
    }

    fun setNeedScrollToBottomWhenViewChange(needToScrollBottom: Boolean) {
        this.isNeedScrollToBottomWhenChange = needToScrollBottom
    }

    /**
     * Set target scroll message id.
     */
    fun setTargetScrollMsgId(msgId: String?) {
        this.targetScrollMsgId = msgId
    }

    /**
     * Scroll to target message.
     */
    fun scrollToTargetMessage(position: Int = -1, highLightAction: (Int) -> Unit) {
        if (position != -1) {
            scrollToPosition(position)
            highLightAction(position)
        } else {
            adapter.mData?.indexOfFirst { it.getMessage().msgId == targetScrollMsgId }?.let {
                smoothScrollToPosition(it)
                highLightAction(it)
            } ?: kotlin.run {
                ChatLog.e(
                    "scrollController",
                    "moveToTarget failed: No original message was found within the scope of the query"
                )
            }
        }
    }

}