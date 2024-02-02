package com.hyphenate.easeui.widget.chatrow

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatDownloadStatus
import com.hyphenate.easeui.common.ChatFileMessageBody
import com.hyphenate.easeui.common.ChatImageMessageBody
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatVideoMessageBody
import com.hyphenate.easeui.common.extensions.getTimestamp
import com.hyphenate.easeui.common.extensions.isFail
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.common.extensions.isSuccess
import com.hyphenate.easeui.common.extensions.loadAvatar
import com.hyphenate.easeui.common.extensions.loadNickname
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.helper.DateFormatHelper
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.feature.chat.config.resetBubbleBackground
import com.hyphenate.easeui.feature.chat.config.setAvatarConfig
import com.hyphenate.easeui.feature.chat.config.setNicknameConfig
import com.hyphenate.easeui.feature.chat.config.setTextMessageMinHeight
import com.hyphenate.easeui.feature.chat.config.setTextMessageTextConfigs
import com.hyphenate.easeui.feature.chat.config.setTimeTextConfig
import com.hyphenate.easeui.feature.chat.interfaces.OnItemBubbleClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListItemClickListener
import com.hyphenate.easeui.model.EaseMessage
import kotlinx.coroutines.launch

abstract class EaseChatRow @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyle: Int = 0,
    val isSender: Boolean
): FrameLayout(context, attrs, defStyle) {

    val inflater by lazy { LayoutInflater.from(context) }

    val timeStampView: TextView? by lazy { findViewById(R.id.timestamp) }
    val userAvatarView: ImageView? by lazy { findViewById(R.id.iv_userhead) }
    val bubbleLayout: ViewGroup? by lazy { findViewById(R.id.bubble) }
    val usernickView: TextView? by lazy { findViewById(R.id.tv_userid) }
    val progressBar: ProgressBar? by lazy { findViewById(R.id.progress_bar) }
    val statusView: ImageView? by lazy { findViewById(R.id.msg_status) }
    val ackedView: TextView? by lazy { findViewById(R.id.tv_ack) }
    val deliveredView: TextView? by lazy { findViewById(R.id.tv_delivered) }
    val selectRadio: RadioButton? by lazy { findViewById(R.id.rb_select) }
    val editView: TextView? by lazy { findViewById(R.id.tv_edit) }

    var message: EaseMessage? = null
    var position: Int = -1

    var itemClickListener: OnMessageListItemClickListener? = null
    private var onItemBubbleClickListener: OnItemBubbleClickListener? = null

    init {
        onInflateView()
        onFindViewById()
    }

    /**
     * Bind data to view.
     * The method is called by ViewHolder.
     */
    fun setUpView(message: EaseMessage?, position: Int) {
        this.message = message
        this.position = position
        setUpBaseView()
        onSetUpView()
    }

    fun updateView() {
        updateMessageByStatus()
    }

    /**
     * Calls by ViewHolder.
     */
    fun setTimestamp(preMessage: EaseMessage?) {
        if (position == 0) {
            preMessage?.run {
                timeStampView?.text = getMessage().getTimestamp(true)
                timeStampView?.visibility = VISIBLE
            }
        } else {
            setOtherTimestamp(preMessage)
        }
    }

    private fun setUpBaseView() {
        setTimestamp(message)
        setItemConfig()
        setAvatarAndNickname()
        updateMessageStatus()
        setThreadRegion()
        setEditStatus()
        setReactionInfo()
        initListener()
    }

    private fun setReactionInfo() {

    }

    private fun setEditStatus() {
        message?.getMessage()?.run {
            editView?.let {
                it.visibility = if (body.operationCount() > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setThreadRegion() {

    }

    private fun updateMessageStatus() {
        updateMessageByStatus()
        updateSendMessageStatus()
    }

    private fun updateMessageByStatus() {
        message?.getMessage()?.run {
            when(status()) {
                ChatMessageStatus.CREATE -> {
                    // When get local messages and check it's status, change the create status to fail.
                }
                ChatMessageStatus.INPROGRESS -> {
                    onMessageInProgress()
                }
                ChatMessageStatus.SUCCESS -> {
                    onMessageSuccess()
                }
                ChatMessageStatus.FAIL -> {
                    onMessageError()
                }
            }
        }
    }

    private fun updateSendMessageStatus() {
        message?.getMessage()?.run {
            if (isSend()) {
                // update sent and delivered status
                deliveredView?.let {
                    it.visibility = View.INVISIBLE
                    if (isSuccess()) {
                        it.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ease_msg_status_sent
                            ),
                            null
                        )
                        it.visibility = VISIBLE
                    }
                    if (ChatClient.getInstance().options.requireDeliveryAck && isDelivered) {
                        it.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            ContextCompat.getDrawable(
                                getContext(),
                                R.drawable.ease_msg_status_received
                            ),
                            null
                        )
                        it.visibility = VISIBLE
                    }
                }
                // update acked status
                ackedView?.let {
                    it.visibility = View.INVISIBLE
                    if (isSuccess() && ChatClient.getInstance().options.requireAck && isAcked) {
                        deliveredView?.visibility = View.INVISIBLE
                        it.visibility = VISIBLE
                    }
                }
                if (isSuccess()) {
                    showSuccessStatus()
                }
                // update error status
                setSendMessageFailStatus()
            }
        }
    }

    private fun updateMessageErrorStatus() {
        ackedView?.visibility = View.INVISIBLE
        deliveredView?.visibility = View.INVISIBLE
        editView?.visibility = View.GONE
        showErrorStatus()
    }

    fun setSendMessageFailStatus() {
        message?.getMessage()?.run {
            if (isSend() && isFail()) {
                statusView?.visibility = View.VISIBLE
            }
        }
    }

    private fun setItemConfig() {
       message?.run {
           getConfig()?.let { config ->
               config.resetBubbleBackground(bubbleLayout, this.getMessage().isSend())
               config.setTimeTextConfig(timeStampView)
               config.setTextMessageTextConfigs(findViewById(R.id.tv_chatcontent))
               config.setAvatarConfig(userAvatarView, this.getMessage().isSend())
               config.setNicknameConfig(usernickView, this.getMessage().isSend())
               config.setTextMessageMinHeight(bubbleLayout)
           }
       }
    }

    /**
     * All user info is from message ext.
     */
    private fun setAvatarAndNickname() {
        message?.getMessage()?.run {
            userAvatarView?.loadAvatar(this)
            usernickView?.loadNickname(this)
        }
    }

    private fun setOtherTimestamp(preMessage: EaseMessage?) {
        if (ChatClient.getInstance().options.isSortMessageByServerTime) {
            if (preMessage != null && DateFormatHelper.isCloseEnough(message!!.getMessage().msgTime
                    , preMessage!!.getMessage().msgTime)) {
                timeStampView?.visibility = GONE
                return
            }
        } else {
            if (preMessage != null && DateFormatHelper.isCloseEnough(message!!.getMessage().localTime()
                    , preMessage!!.getMessage().localTime())) {
                timeStampView?.visibility = GONE
                return
            }
        }

        message?.run {
            timeStampView?.text = getMessage().getTimestamp(true)
            timeStampView?.visibility = VISIBLE
        }
    }

    private fun initListener() {
        bubbleLayout?.let {
            it.setOnClickListener {
                if (itemClickListener?.onBubbleClick(message?.getMessage()) == true) {
                    return@setOnClickListener
                }
                onItemBubbleClickListener?.onBubbleClick(message)
            }
            it.setOnLongClickListener { view ->
                if (itemClickListener?.onBubbleLongClick(view, message?.getMessage()) == true) {
                    return@setOnLongClickListener true
                }
                return@setOnLongClickListener false
            }
        }
        statusView?.let {
            it.setOnClickListener {
                itemClickListener?.onResendClick(message?.getMessage())
            }
        }
        userAvatarView?.let {
            it.setOnClickListener {
                message?.getMessage()?.run {
                    itemClickListener?.onUserAvatarClick(if (isSend()) ChatClient.getInstance().currentUser else from)
                }
            }
            it.setOnLongClickListener {
                message?.getMessage()?.run {
                    if (itemClickListener != null) {
                        itemClickListener?.onUserAvatarLongClick(if (isSend()) ChatClient.getInstance().currentUser else from)
                        return@setOnLongClickListener true
                    }
                }
                return@setOnLongClickListener false
            }
        }
        message?.getMessage()?.let {
            if (it.status() == ChatMessageStatus.INPROGRESS) {
                message?.setMessageStatusCallback(CallbackImpl(
                    onSuccess = {
                        context.mainScope().launch {
                            onMessageSuccess()
                        }
                    },
                    onError = { code, error ->
                        context.mainScope().launch {
                            onMessageError()
                        }
                    },
                    onProgress = { progress ->
                        context.mainScope().launch {
                            onMessageInProgress()
                        }
                    }
                ))
            }
            if (it.isSuccess() && it.body is ChatFileMessageBody) {
                // If the thumbnail is downloading, set the callback to update the UI.
                if ((it.body is ChatImageMessageBody
                            && (it.body as ChatImageMessageBody).thumbnailDownloadStatus() == ChatDownloadStatus.DOWNLOADING)
                    || (it.body is ChatVideoMessageBody
                            && (it.body as ChatVideoMessageBody).thumbnailDownloadStatus() == ChatDownloadStatus.DOWNLOADING)) {

                    message?.setDownloadStatusCallback(CallbackImpl(
                        onSuccess = {
                            context.mainScope().launch {
                                onMessageSuccess()
                            }
                        },
                        onError = { code, error ->
                            context.mainScope().launch {
                                onMessageError()
                            }
                        },
                        onProgress = { progress ->
                            context.mainScope().launch {
                                onMessageInProgress()
                            }
                        }
                    ))
                }
            }
        }
    }

    /**
     * Show success status.
     */
    protected open fun showSuccessStatus() {
        progressBar?.visibility = INVISIBLE
        statusView?.visibility = INVISIBLE
    }

    protected open fun showErrorStatus() {
        progressBar?.visibility = INVISIBLE
        statusView?.visibility = VISIBLE
    }

    protected open fun showInProgressStatus() {
        progressBar?.visibility = VISIBLE
        statusView?.visibility = INVISIBLE
    }
    protected open fun onMessageSuccess() {
        updateSendMessageStatus()
    }

    protected open fun onMessageError() {
        updateMessageErrorStatus()
    }

    protected open fun onMessageInProgress() {
        showInProgressStatus()
    }

    protected open fun isSend():Boolean {
        return isSender
    }

    /**
     * Set message item click listeners.
     * @param listener
     */
    fun setOnMessageListItemClickListener(listener: OnMessageListItemClickListener?) {
        itemClickListener = listener
    }

    fun setOnItemBubbleClickListener(listener: OnItemBubbleClickListener?) {
        this.onItemBubbleClickListener = listener
    }

    /**
     * Override it and inflate your view in this method.
     */
    abstract fun onInflateView()

    /**
     * Override it and find view by id in this method.
     */
    open fun onFindViewById() {}

    /**
     * Override it and set data or listener in this method.
     */
    abstract fun onSetUpView()

    companion object {
        const val TAG = "EaseChatRow"
    }

}