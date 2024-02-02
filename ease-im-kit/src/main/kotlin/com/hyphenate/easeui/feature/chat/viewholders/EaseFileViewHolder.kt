package com.hyphenate.easeui.feature.chat.viewholders

import android.net.Uri
import android.view.View
import com.hyphenate.easeui.feature.chat.activities.EaseShowNormalFileActivity
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.common.ChatNormalFileMessageBody
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.utils.EaseCompat
import com.hyphenate.easeui.common.utils.EaseFileUtils
import com.hyphenate.easeui.model.EaseMessage

class EaseFileViewHolder(itemView: View) : EaseChatRowViewHolder(itemView) {
    override fun onBubbleClick(message: EaseMessage?) {
        super.onBubbleClick(message)
        val fileMessageBody = message?.getMessage()?.body as ChatNormalFileMessageBody
        val filePath: Uri? = fileMessageBody.localUri
        EaseFileUtils.takePersistableUriPermission(mContext, filePath)
        if (EaseFileUtils.isFileExistByUri(mContext, filePath)) {
            EaseCompat.openFile(mContext, filePath!!)
        } else {
            // download the file
            EaseShowNormalFileActivity.actionStart(mContext, message?.getMessage())
        }
        message?.getMessage()?.run {
            if(direct() == ChatMessageDirection.RECEIVE && !isAcked && chatType == ChatType.Chat) {
                try {
                    ChatClient.getInstance().chatManager()
                        .ackMessageRead(from, msgId)
                } catch (e: ChatException) {
                    e.printStackTrace()
                }
            }
        }
    }
}