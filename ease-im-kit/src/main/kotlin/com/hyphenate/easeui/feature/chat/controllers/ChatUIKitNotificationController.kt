package com.hyphenate.easeui.feature.chat.controllers

import android.view.View
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.databinding.UikitLayoutChatBinding
import com.hyphenate.easeui.feature.chat.widgets.ChatUIKitUnreadNotificationView
import com.hyphenate.easeui.viewmodel.messages.IChatViewRequest
import kotlinx.coroutines.launch

class ChatUIKitNotificationController(
    private val binding: UikitLayoutChatBinding,
    private val conversationId: String?,
    private val viewModel: IChatViewRequest?
) {

    private var shouldDismiss: Boolean = false

    init {
        if (ChatUIKitClient.getConfig()?.chatConfig?.showUnreadNotificationInChat == true && !shouldDismiss) {
            showNotificationView(false)
            binding.layoutNotification.setOnNotificationClickListener {
                dismissNotificationView()
                binding.layoutChatMessage.isCanAutoScrollToBottom = true
                binding.layoutChatMessage.refreshToLatest()
            }
        }
    }

    fun showNotificationView(isShow: Boolean) {
        if (ChatUIKitClient.getConfig()?.chatConfig?.showUnreadNotificationInChat == false || shouldDismiss) return
        binding.layoutNotification.context.mainScope().launch {
            binding.layoutNotification.visibility = if (isShow) {
                updateNotificationView()
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    fun isNotificationViewShow(): Boolean {
        return binding.layoutNotification.visibility == View.VISIBLE
    }

    fun updateNotificationView() {
        if (binding.layoutChatMessage.isCanAutoScrollToBottom || shouldDismiss) return
        binding.layoutNotification.context.mainScope().launch {
            binding.layoutNotification.getNotificationView()?.let {
                if (it is ChatUIKitUnreadNotificationView) {
                    ChatClient.getInstance().chatManager().getConversation(conversationId)?.let { conv ->
                        binding.root.context.mainScope().launch {
                            it.updateUnreadCount(conv.unreadMsgCount)
                        }
                    }
                }
            }
        }
    }

    fun dismissNotificationView() {
        if (ChatUIKitClient.getConfig()?.chatConfig?.showUnreadNotificationInChat == false || shouldDismiss) return
        showNotificationView(false)
        binding.layoutChatMessage.isCanAutoScrollToBottom = true
        ChatClient.getInstance().chatManager().getConversation(conversationId)?.let {
            if (it.unreadMsgCount > 0) {
                it.markAllMessagesAsRead()
                viewModel?.sendChannelAck()
            }
        }
    }

    fun setShouldDismiss(shouldDismiss: Boolean) {
        this.shouldDismiss = shouldDismiss
    }

}