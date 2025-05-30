package com.hyphenate.easeui.feature.chat.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Toast
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.ChatUIKitBaseActivity
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatVideoMessageBody
import com.hyphenate.easeui.common.extensions.hasRoute
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.common.utils.ChatUIKitFileUtils
import com.hyphenate.easeui.common.utils.StatusBarCompat
import com.hyphenate.easeui.databinding.UikitShowvideoActivityBinding
import kotlinx.coroutines.launch
import java.io.File

/**
 * show the video
 *
 */
class ChatUIKitShowVideoActivity : ChatUIKitBaseActivity<UikitShowvideoActivityBinding>() {
    private var localFilePath: Uri? = null

    override fun beforeSetContentView() {
        super.beforeSetContentView()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        StatusBarCompat.hideStatusBar(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message: ChatMessage? = intent.getParcelableExtra("msg")
        if (message?.body !is ChatVideoMessageBody) {
            Toast.makeText(
                this@ChatUIKitShowVideoActivity,
                "Unsupported message body",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        val messageBody: ChatVideoMessageBody? = message.body as? ChatVideoMessageBody
        localFilePath = messageBody?.localUri
        ChatLog.d(TAG, "localFilePath = $localFilePath")
        ChatLog.d(TAG, "local filename = ${messageBody?.fileName}")

        //Check Uri read permissions
        ChatUIKitFileUtils.takePersistableUriPermission(this, localFilePath)
        if (ChatUIKitFileUtils.isFileExistByUri(this, localFilePath)) {
            showLocalVideo(localFilePath)
        } else {
            ChatLog.d(TAG, "download remote video file")
            downloadVideo(message)
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): UikitShowvideoActivityBinding? {
        return UikitShowvideoActivityBinding.inflate(inflater)
    }

    override fun setActivityTheme() {}
    private fun showLocalVideo(videoUri: Uri?) {
        ChatUIKitShowLocalVideoActivity.actionStart(this, videoUri)
        finish()
    }

    /**
     * download video file
     */
    private fun downloadVideo(message: ChatMessage) {
        binding.loadingLayout.visibility = View.VISIBLE
        message.setMessageStatusCallback(CallbackImpl(
            onSuccess = {
                mainScope().launch {
                    binding.loadingLayout.visibility = View.GONE
                    binding.progressBar.progress = 0
                    showLocalVideo((message.body as ChatVideoMessageBody).localUri)
                }
            },
            onError = { code, error ->
                ChatLog.e("###", "offline file transfer error:$message")
                val localFilePath: Uri = (message.getBody() as ChatVideoMessageBody).localUri
                val filePath: String =
                    ChatUIKitFileUtils.getFilePath(this@ChatUIKitShowVideoActivity, localFilePath)
                if (TextUtils.isEmpty(filePath)) {
                    this@ChatUIKitShowVideoActivity.getContentResolver()
                        .delete(localFilePath, null, null)
                } else {
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                mainScope().launch {
                    if (code == ChatError.FILE_NOT_FOUND) {
                        Toast.makeText(
                            mContext,
                            R.string.uikit_video_expired,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onProgress = { progress ->
                mainScope().launch {
                    binding.progressBar.progress = progress
                }
            }
        ))
        ChatClient.getInstance().chatManager().downloadAttachment(message)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        private const val TAG = "ShowVideoActivity"
        fun actionStart(context: Context, message: ChatMessage?) {
            val intent = Intent(context, ChatUIKitShowVideoActivity::class.java)
            intent.putExtra("msg", message)
            ChatUIKitClient.getCustomActivityRoute()?.getActivityRoute(intent.clone() as Intent)?.let {
                if (it.hasRoute()) {
                    context.startActivity(it)
                    return
                }
            }
            context.startActivity(intent)
        }
    }
}