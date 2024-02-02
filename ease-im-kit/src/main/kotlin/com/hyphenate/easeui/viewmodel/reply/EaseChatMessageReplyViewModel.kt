package com.hyphenate.easeui.viewmodel.reply

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatFileMessageBody
import com.hyphenate.easeui.common.ChatImageMessageBody
import com.hyphenate.easeui.common.ChatLocationMessageBody
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.ChatVideoMessageBody
import com.hyphenate.easeui.common.ChatVoiceMessageBody
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.extensions.getEmojiText
import com.hyphenate.easeui.common.extensions.getUserCardInfo
import com.hyphenate.easeui.common.extensions.getUserInfo
import com.hyphenate.easeui.common.extensions.isUserCardMessage
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.utils.EaseFileUtils
import com.hyphenate.easeui.feature.chat.reply.interfaces.IChatMessageReplyResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.model.getNickname

open class EaseChatMessageReplyViewModel: ViewModel(), IChatMessageReplyRequest {
    private var mView: IChatMessageReplyResultView? = null
    override fun attachView(view: IControlDataView) {
        mView = view as? IChatMessageReplyResultView
    }

    override fun showQuoteMessageInfo(message: ChatMessage?) {
        if (message == null || message.body == null) {
            mView?.onShowError(ChatError.GENERAL_ERROR, "Message or body cannot be null.")
            return
        }
        val user: EaseUser? = message.getUserInfo()?.toUser()
        var from:String? = null
        from = if (user == null) {
            message.from
        } else {
            user.getNickname()
        }
        mView?.showQuoteMessageNickname(from)
        val builder = StringBuilder()
        var localPath: String? = null
        var remoteUrl: String? = null
        when (message.type) {
            ChatMessageType.TXT -> if (message.getBooleanAttribute(
                    EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION,
                    false
                )
            ) {
                builder.append(EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_emoji_type))
            } else {
                val textBody = message.body as ChatTextMessageBody
                builder.append(
                    "$from: ${textBody.message.getEmojiText(EaseIM.getContext()!!)}"
                    ).toString()
            }

            ChatMessageType.VOICE -> {
                val voiceBody = message.body as ChatVoiceMessageBody
                builder.append(EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_voice_type))
                    .append(": ")
                    .append((if (voiceBody.length > 0) voiceBody.length else 0).toString() + "\"")
                mView?.showQuoteMessageAttachment(
                    ChatMessageType.VOICE,
                    null,
                    null,
                    R.drawable.ease_chatfrom_voice_playing
                )
            }

            ChatMessageType.VIDEO -> {
                builder.append(EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_video_type))
                val videoBody = message.body as ChatVideoMessageBody
                videoBody?.let {
                    if (!TextUtils.isEmpty(it.localThumb) && EaseFileUtils.isFileExistByUri(
                            EaseIM.getContext(),
                            it.localThumbUri
                        )
                    ) {
                        localPath = it.localThumb
                    }
                    remoteUrl = it.thumbnailUrl
                    mView?.showQuoteMessageAttachment(
                        ChatMessageType.VIDEO,
                        localPath,
                        remoteUrl,
                        R.drawable.ease_chat_quote_icon_video
                    )
                }

            }

            ChatMessageType.FILE -> {
                val fileBody = message.body as ChatFileMessageBody
                builder.append(EaseIM.getContext()?.getResources()?.getString(R.string.ease_message_reply_file_type))
                    .append(" ")
                    .append(fileBody.fileName)
                mView?.showQuoteMessageAttachment(
                    ChatMessageType.FILE,
                    null,
                    null,
                    R.drawable.ease_chat_quote_message_attachment
                )
            }

            ChatMessageType.IMAGE -> {
                builder.append(EaseIM.getContext()?.getResources()?.getString(R.string.ease_message_reply_image_type))
                val imageBody = message.body as ChatImageMessageBody
                imageBody?.let {
                    if (!imageBody.thumbnailUrl.isNullOrEmpty() && EaseFileUtils.isFileExistByUri(
                            EaseIM.getContext(), imageBody.thumbnailLocalUri())) {
                        localPath = imageBody.thumbnailLocalPath()
                    } else if (!imageBody.localUrl.isNullOrEmpty() && EaseFileUtils.isFileExistByUri(
                        EaseIM.getContext(), imageBody.localUri)) {
                        localPath = imageBody.localUrl
                    }
                    remoteUrl = imageBody.remoteUrl
                    mView?.showQuoteMessageAttachment(
                        ChatMessageType.IMAGE,
                        localPath,
                        remoteUrl,
                        R.drawable.ease_chat_quote_icon_image
                    )
                }
            }

            ChatMessageType.LOCATION -> {
                val locationBody = message.body as ChatLocationMessageBody
                builder.append(EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_location_type))
                if (locationBody != null && !TextUtils.isEmpty(locationBody.address)) {
                    builder.append(": ").append(locationBody.address)
                }
            }

            ChatMessageType.CUSTOM -> {
                if (message.isUserCardMessage()) {
                    mView?.showQuoteMessageAttachment(
                        ChatMessageType.CUSTOM,
                        null,
                        null,
                        R.drawable.ease_chat_quote_icon_user_card
                    )
                    builder.append(message.getUserCardInfo()?.name)
                } else {
                    builder.append(EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_custom_type))
                }
            }
            ChatMessageType.COMBINE -> builder.append(
                EaseIM.getContext()?.resources?.getString(R.string.ease_message_reply_combine_type)
            )

            else -> {}
        }
        mView?.showQuoteMessageContent(builder)
    }

}