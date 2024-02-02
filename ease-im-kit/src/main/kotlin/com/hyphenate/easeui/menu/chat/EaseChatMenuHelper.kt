package com.hyphenate.easeui.menu.chat

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.extensions.canEdit
import com.hyphenate.easeui.interfaces.OnMenuChangeListener
import com.hyphenate.easeui.interfaces.OnMenuDismissListener
import com.hyphenate.easeui.interfaces.OnMenuItemClickListener
import com.hyphenate.easeui.menu.EaseMenuHelper
import com.hyphenate.easeui.menu.EaseMenuItemView
import com.hyphenate.easeui.model.EaseMenuItem

class EaseChatMenuHelper: EaseMenuHelper() {
    private var message: ChatMessage? = null
    private var onMenuChangeListener: OnMenuChangeListener? = null
    fun initMenuWithMessage(message: ChatMessage?) {
        this.message = message
        setMenuOrientation(EaseMenuItemView.MenuOrientation.HORIZONTAL)
        setMenuGravity(EaseMenuItemView.MenuGravity.LEFT)
        showCancel(false)
        setDefaultMenus()
        setMenuVisibleByMessageType()
        onMenuChangeListener?.onPreMenu(this, message)
        setOnMenuDismissListener(object : OnMenuDismissListener {
            override fun onDismiss() {
                onMenuChangeListener?.onDismiss()
            }
        })
    }

    private fun setMenuVisibleByMessageType() {
        message?.let {
            val type: ChatMessageType = it.type
            setAllItemsVisible(false)
            findItemVisible(R.id.action_chat_delete, true)
            findItem(R.id.action_chat_delete)
                ?.title = getContext()?.getString(R.string.ease_action_delete)
            if (it.status() == ChatMessageStatus.SUCCESS && it.direct() === ChatMessageDirection.SEND) {
                findItemVisible(R.id.action_chat_recall, canRecallMessage(it))
            }
            if (it.status() == ChatMessageStatus.SUCCESS && it.from != ChatClient.getInstance().currentUser)
                findItemVisible(R.id.action_chat_report, true)
            if (type == ChatMessageType.TXT) findItemVisible(R.id.action_chat_copy, true)
            if (it.chatType === ChatType.GroupChat && it.chatThread == null) {
                findItemVisible(R.id.action_chat_thread, true)
            }

            if (it.direct() === ChatMessageDirection.RECEIVE) {
                findItemVisible(R.id.action_chat_recall, false)
            }
            if (it.status() !== ChatMessageStatus.SUCCESS) {
                findItemVisible(R.id.action_chat_recall, false)
                findItemVisible(R.id.action_chat_thread, false)
            }
            findItemVisible(
                R.id.action_chat_reply,
                it.status() === ChatMessageStatus.SUCCESS
                        && EaseIM.getConfig()?.chatConfig?.enableReplyMessage == true
            )
            findItemVisible(
                R.id.action_chat_select,
                it.status() === ChatMessageStatus.SUCCESS
                        && EaseIM.getConfig()?.chatConfig?.enableSendCombineMessage == true
            )
            findItemVisible(R.id.action_chat_edit, it.canEdit())
        }

    }

    private fun canRecallMessage(message: ChatMessage): Boolean {
        EaseIM.getConfig()?.chatConfig?.timePeriodCanRecallMessage?.let {
            if (it != -1L && it > 0) return System.currentTimeMillis() - message.localTime() <= it
        }
        return true
    }

    private fun setDefaultMenus() {
        clear()
        MENU_ITEM_IDS.forEachIndexed { index, item ->
            getContext()?.let {
                addItemMenu(item, (index + 1) * 10, it.getString(MENU_TITLES[index]), resourceId = MENU_ICONS[index])
            }
        }
    }


    fun setOnMenuChangeListener(listener: OnMenuChangeListener?) {
        this.onMenuChangeListener = listener
    }

    override fun setOnMenuItemClickListener(listener: OnMenuItemClickListener?) {
        super.setOnMenuItemClickListener(object : OnMenuItemClickListener {
            override fun onMenuItemClick(item: EaseMenuItem?, position: Int): Boolean {
                if (onMenuChangeListener?.onMenuItemClick(item, message) == true) {
                    return true
                }
                return listener?.onMenuItemClick(item, position) == true
            }
        })

    }

    companion object {
        val MENU_ITEM_IDS = intArrayOf(
            R.id.action_chat_copy,
            R.id.action_chat_reply,
            R.id.action_chat_edit,
            R.id.action_chat_report,
            R.id.action_chat_delete,
            R.id.action_chat_recall
        )
        val MENU_TITLES = intArrayOf(
            R.string.ease_action_copy,
            R.string.ease_action_reply,
            R.string.ease_action_edit,
            R.string.ease_action_report,
            R.string.ease_action_delete,
            R.string.ease_action_unsent
        )
        val MENU_ICONS = intArrayOf(
            R.drawable.ease_chat_item_menu_copy,
            R.drawable.ease_chat_item_menu_reply,
            R.drawable.ease_chat_item_menu_edit,
            R.drawable.ease_chat_item_menu_report,
            R.drawable.ease_chat_item_menu_delete,
            R.drawable.ease_chat_item_menu_unsent
        )
    }

}