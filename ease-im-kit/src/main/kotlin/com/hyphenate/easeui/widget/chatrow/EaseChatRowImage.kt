package com.hyphenate.easeui.widget.chatrow

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatDownloadStatus
import com.hyphenate.easeui.common.ChatImageMessageBody
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.extensions.getImageShowSize
import com.hyphenate.easeui.common.extensions.isFileExist
import com.hyphenate.easeui.common.extensions.isSend
import com.hyphenate.easeui.common.extensions.isSuccess
import com.hyphenate.easeui.common.extensions.loadImageFromMessage
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.utils.EaseFileUtils
import kotlinx.coroutines.launch

/**
 * image for row
 */
class EaseChatRowImage @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0,
    isSender: Boolean = false
) : EaseChatRowFile(context, attrs, defStyleAttr, isSender) {
    protected val imageView: ImageView? by lazy { findViewById(R.id.image) }
    private var retryTimes = 3

    override fun onInflateView() {
        inflater.inflate(
            if (!isSender) R.layout.ease_row_received_picture else R.layout.ease_row_sent_picture,
            this
        )
    }

    override fun onSetUpView() {
        bubbleLayout?.background = null
        message?.getMessage()?.run {
            (body as? ChatImageMessageBody)?.let {
                if (isSuccess() && it.thumbnailDownloadStatus() == ChatDownloadStatus.DOWNLOADING) {
                    setMessageDownloadCallback(true)
                }
                retryTimes = 3
                if (EaseFileUtils.isFileExistByUri(context, it.localUri)
                    || EaseFileUtils.isFileExistByUri(context, it.thumbnailLocalUri())) {
                    showImageView(this, position)
                    return
                }
                checkAttachmentStatus(position)
            }
        }
    }

    private fun checkAttachmentStatus(position: Int) {
        message?.getMessage()?.run {
            showPlaceholderSize(this)
            // If auto transfer message attachments to Chat Server, then download attachments
            if (ChatClient.getInstance().options.autoTransferMessageAttachments) {
                val imgBody = body as ChatImageMessageBody
                // received messages
                if (!isSend()) {
                    if (ChatClient.getInstance().options.autodownloadThumbnail) {
                        if (imgBody.thumbnailDownloadStatus() === ChatDownloadStatus.SUCCESSED) {
                            if (imgBody.thumbnailLocalUri()?.isFileExist(context) == true) {
                                showSuccessStatus()
                                showImageView(this, position)
                                return
                            }
                        } else if (imgBody.thumbnailDownloadStatus() === ChatDownloadStatus.DOWNLOADING) {
                            // Listener to ChatMessageListener#onMessageChanged, then update the UI.
                            // Note: If you set the method ChatMessage#setMessageStatusCallback,
                            // ChatMessageListener#onMessageChanged will not be called.
                            showInProgressStatus()
                            return
                        }
                    }
                }
                if (status() != ChatMessageStatus.SUCCESS) {
                    return
                }
                if (imgBody.downloadStatus() == ChatDownloadStatus.SUCCESSED
                    || imgBody.thumbnailDownloadStatus() == ChatDownloadStatus.SUCCESSED
                ) {
                    if (EaseFileUtils.isFileExistByUri(context, imgBody.localUri)
                        || EaseFileUtils.isFileExistByUri(context, imgBody.thumbnailLocalUri())) {
                        showSuccessStatus()
                        showImageView(this, position)
                        return
                    }
                } else if (imgBody.downloadStatus() == ChatDownloadStatus.DOWNLOADING
                    || imgBody.thumbnailDownloadStatus() == ChatDownloadStatus.DOWNLOADING
                ) {
                    return
                }
                if (isInRetrying) {
                    downloadAttachment(!EaseFileUtils.isFileExistByUri(context, imgBody.thumbnailLocalUri()) && !isSend())
                }
            } else {
                showImageView(this, position)
            }
        }
    }

    private fun showPlaceholderSize(message: ChatMessage) {
        message.getImageShowSize(context)?.let {
            imageView?.layoutParams = ViewGroup.LayoutParams(it.width, it.height)
        }
    }

    private val isInRetrying: Boolean
        private get() {
            val times = retryTimes
            retryTimes--
            return times > 0
        }

    override fun onMessageSuccess() {
        super.onMessageSuccess()
        //Even if it's the sender, it needs to be executed after
        // it's successfully sent to prevent the image size from being wrong
        message?.getMessage()?.body?.let {
            val imgBody = it as ChatImageMessageBody
            if (imgBody.thumbnailDownloadStatus() == ChatDownloadStatus.SUCCESSED
                || imgBody.thumbnailDownloadStatus() == ChatDownloadStatus.FAILED) {
                showImageView(message?.getMessage(), position)
            }
        }

    }

    override fun onMessageInProgress() {
        message?.getMessage()?.run {
            if (isSend()) {
                super.onMessageInProgress()
            } else {
                if (ChatClient.getInstance().options.autodownloadThumbnail) {
                    //imageView.setImageResource(R.drawable.ease_default_image);
                } else {
                    progressBar?.visibility = INVISIBLE
                }
            }
        }
    }

    override fun onDownloadAttachmentSuccess() {
        showSuccessStatus()
        showImageView(message?.getMessage(), position)
    }

    override fun onDownloadAttachmentError(code: Int, error: String?) {
        ChatLog.e(
            EaseChatRowImage::class.java.simpleName,
            "onDownloadAttachmentError:$code, error:$error"
        )
        context.mainScope().launch {
            imageView?.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            imageView?.setImageResource(R.drawable.ease_default_image)
        }
    }

    override fun onDownloadAttachmentProgress(progress: Int) {
        showInProgressStatus()
    }

    /**
     * load image into image view
     *
     */
    @SuppressLint("StaticFieldLeak")
    protected fun showImageView(message: ChatMessage?, position: Int) {
        if (position == this.position && TextUtils.equals(
                message?.msgId,
                this.message?.getMessage()?.msgId
            )
        ) {
            imageView?.loadImageFromMessage(message, onSuccess = {
                showSuccessStatus()
            }, onError = {
                showErrorStatus()
            })
        }
    }
}