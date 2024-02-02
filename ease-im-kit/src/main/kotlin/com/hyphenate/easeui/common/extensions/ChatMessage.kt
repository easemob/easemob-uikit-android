package com.hyphenate.easeui.common.extensions

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatCustomMessageBody
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatImageMessageBody
import com.hyphenate.easeui.common.ChatImageUtils
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.ChatVideoMessageBody
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.EaseConstant.MESSAGE_EXT_USER_INFO_AVATAR_KEY
import com.hyphenate.easeui.common.EaseConstant.MESSAGE_EXT_USER_INFO_NICKNAME_KEY
import com.hyphenate.easeui.common.helper.DateFormatHelper
import com.hyphenate.easeui.configs.EaseDateFormatConfig
import com.hyphenate.easeui.feature.invitation.enums.InviteMessageStatus
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseSize
import com.hyphenate.easeui.provider.getSyncProfile
import com.hyphenate.easeui.provider.getSyncUser
import org.json.JSONObject

internal fun ChatMessage.getMessageDigest(context: Context): String {
    return when(type) {
        ChatMessageType.LOCATION -> {
            if (direct() == ChatMessageDirection.RECEIVE) {
                var name = from
                getSyncUserFromProvider()?.let { profile ->
                    name = profile.name
                }
                context.getString(R.string.ease_location_recv, name)
            } else {
                context.getString(R.string.ease_location_prefix)
            }
        }
        ChatMessageType.IMAGE -> {
            context.getString(R.string.ease_picture)
        }
        ChatMessageType.VIDEO -> {
            context.getString(R.string.ease_video)
        }
        ChatMessageType.VOICE -> {
            context.getString(R.string.ease_voice)
        }
        ChatMessageType.FILE -> {
            context.getString(R.string.ease_file)
        }
        ChatMessageType.CUSTOM -> {
            if (isUserCardMessage()) {
                context.getString(R.string.ease_user_card, getUserCardInfo()?.name ?: "")
            } else if (isAlertMessage()) {
                (body as ChatCustomMessageBody).params[EaseConstant.MESSAGE_CUSTOM_ALERT_CONTENT]
                    ?: context.getString(R.string.ease_custom)
            } else {
                context.getString(R.string.ease_custom)
            }
        }
        ChatMessageType.TXT -> {
            (body as ChatTextMessageBody).let {
                getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false).let { isBigExp ->
                    if(isBigExp) {
                        if (it.message.isNullOrEmpty()) {
                            context.getString(R.string.ease_dynamic_expression)
                        } else {
                            it.message
                        }
                    } else {
                        it.message
                    }
                } ?: it.message
            }
        }
        ChatMessageType.COMBINE -> {
            context.getString(R.string.ease_combine)
        }
        else -> {
            ""
        }
    }
}

internal fun ChatMessage.getSyncUserFromProvider(): EaseProfile? {
    return if (chatType == ChatType.Chat) {
        if (direct() == ChatMessageDirection.RECEIVE) {
            // Get user info from conversation info provider first.
            // Then get user info from user provider.
            EaseIM.getConversationInfoProvider()?.getSyncProfile(from, ChatConversationType.Chat)
                ?: EaseIM.getUserProvider()?.getSyncUser(from)
        } else {
            EaseIM.getCurrentUser()
        }
    } else if (chatType == ChatType.GroupChat) {
        if (direct() == ChatMessageDirection.RECEIVE) {
            // Get user info from conversation info provider first.
            // Then get user info from user provider.
            EaseIM.getGroupMemberProfileProvider()?.getMemberProfile(conversationId(), from)
                ?: EaseIM.getUserProvider()?.getSyncUser(from)
        } else {
            EaseIM.getCurrentUser()
        }
    } else {
        null
    }
}

/**
 * Create a local message when unsent a message or receive a unsent message.
 */
internal fun ChatMessage.createUnsentMessage(isReceive: Boolean = false): ChatMessage {
    val msgNotification = if (isReceive) {
        ChatMessage.createReceiveMessage(ChatMessageType.TXT)
    } else {
        ChatMessage.createSendMessage(ChatMessageType.TXT)
    }

    val text: String = if (isSend()) {
        EaseIM.getContext()?.resources?.getString(R.string.ease_msg_recall_by_self)
            ?: ""
    } else {
        EaseIM.getContext()?.resources?.getString(R.string.ease_msg_recall_by_user, getUserInfo()?.name ?: from)
            ?: ""
    }
    val txtBody = ChatTextMessageBody(
        text
    )
    msgNotification.msgId = msgId
    msgNotification.addBody(txtBody)
    msgNotification.to = to
    msgNotification.from = from
    msgNotification.msgTime = msgTime
    msgNotification.chatType = chatType
    msgNotification.setLocalTime(localTime())
    msgNotification.setAttribute(EaseConstant.MESSAGE_TYPE_RECALL, true)
    msgNotification.setStatus(ChatMessageStatus.SUCCESS)
    msgNotification.setIsChatThreadMessage(isChatThreadMessage)
    return msgNotification
}

/**
 * Get the String timestamp from [ChatMessage].
 */
fun ChatMessage.getTimestamp(isChat: Boolean = false): String? {
    val timestamp = if (ChatClient.getInstance().options.isSortMessageByServerTime) msgTime else localTime()
    if (isChat) {
        return if (DateFormatHelper.isSameDay(timestamp)) {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.chatTodayFormat
                ?: EaseDateFormatConfig.DEFAULT_CHAT_TODAY_FORMAT)
        } else if (DateFormatHelper.isSameYear(timestamp)) {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.chatOtherDayFormat
                    ?: EaseDateFormatConfig.DEFAULT_CHAT_OTHER_DAY_FORMAT)
        } else {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.chatOtherYearFormat
                    ?: EaseDateFormatConfig.DEFAULT_CHAT_OTHER_YEAR_FORMAT)
        }
    } else {
        return if (DateFormatHelper.isSameDay(timestamp)) {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.convTodayFormat
                    ?: EaseDateFormatConfig.DEFAULT_CONV_TODAY_FORMAT)
        } else if (DateFormatHelper.isSameYear(timestamp)) {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.convOtherDayFormat
                    ?: EaseDateFormatConfig.DEFAULT_CONV_OTHER_DAY_FORMAT)
        } else {
            DateFormatHelper.timestampToDateString(timestamp
                , EaseIM.getConfig()?.dateFormatConfig?.convOtherYearFormat
                    ?: EaseDateFormatConfig.DEFAULT_CONV_OTHER_YEAR_FORMAT)
        }
    }
}

/**
 * Check whether the message is a user card message.
 */
fun ChatMessage.isUserCardMessage(): Boolean {
    val event = (body as? ChatCustomMessageBody)?.event() ?: ""
    return event == EaseConstant.USER_CARD_EVENT
}

/**
 * Check whether the message is a alert message.
 */
fun ChatMessage.isAlertMessage(): Boolean {
    val event = (body as? ChatCustomMessageBody)?.event() ?: ""
    return event == EaseConstant.MESSAGE_CUSTOM_ALERT
}

/**
 * Get user card info from message.
 */
fun ChatMessage.getUserCardInfo(): EaseProfile? {
    if (isUserCardMessage()) {
        (body as? ChatCustomMessageBody)?.let {
            val params: Map<String, String> = it.params
            val uId = params[EaseConstant.USER_CARD_ID]
            val nickname = params[EaseConstant.USER_CARD_NICK]
            val headUrl = params[EaseConstant.USER_CARD_AVATAR]
            if (uId.isNullOrEmpty()) return null
            return EaseProfile(uId, nickname, headUrl)
        }
    }
    return null
}

internal fun ChatMessage.isGroupChat(): Boolean {
    return chatType == ChatType.GroupChat
}

/**
 * Check whether the message is sent by current user.
 */
internal fun ChatMessage.isSend(): Boolean {
    return direct() == ChatMessageDirection.SEND
}

/**
 * Add userinfo to message when sending message.
 */
internal fun ChatMessage.addUserInfo(nickname: String?, avatarUrl: String?) {
    if (nickname.isNullOrEmpty() && avatarUrl.isNullOrEmpty()) {
        return
    }
    val info = JSONObject()
    if (!nickname.isNullOrEmpty()) info.put(MESSAGE_EXT_USER_INFO_NICKNAME_KEY, nickname)
    if (!avatarUrl.isNullOrEmpty()) info.put(MESSAGE_EXT_USER_INFO_AVATAR_KEY, avatarUrl)
    setAttribute(EaseConstant.MESSAGE_EXT_USER_INFO_KEY, info)
}

/**
 * Parse userinfo from message when receiving a message.
 */
internal fun ChatMessage.getUserInfo(updateCache: Boolean = false): EaseProfile? {
    if (!updateCache) {
        if (EaseIM.getCache().getMessageUserInfo(from) != null) return EaseIM.getCache().getMessageUserInfo(from)
    }
    try {
        getJSONObjectAttribute(EaseConstant.MESSAGE_EXT_USER_INFO_KEY)?.let { info ->
            val profile = EaseProfile(
                id = from,
                name = info.optString(MESSAGE_EXT_USER_INFO_NICKNAME_KEY),
                avatar = info.optString(MESSAGE_EXT_USER_INFO_AVATAR_KEY)
            )
            profile.setTimestamp(msgTime)
            EaseIM.getCache().insertMessageUser(from, profile)
            return profile
        }
    } catch (e: ChatException) {
        e.printStackTrace()
    }
    return EaseProfile(from)
}

internal fun ChatMessage.getThumbnailLocalUri(context: Context): Uri? {
    var imageUri: Uri? = null
    if (type == ChatMessageType.IMAGE) {
        (body as ChatImageMessageBody).let {
            imageUri = it.localUri
            imageUri?.takePersistablePermission(context)
            if (imageUri?.isFileExist(context) == false) {
                imageUri = it.thumbnailLocalUri()
                imageUri?.takePersistablePermission(context)
                if (imageUri?.isFileExist(context) == false) {
                    imageUri = null
                    ChatLog.e("getImageShowSize", "image file not exist")
                }
            }
        }
    } else if (type == ChatMessageType.VIDEO) {
        (body as ChatVideoMessageBody).let {
            imageUri = it.localThumbUri
            imageUri?.takePersistablePermission(context)
            if (imageUri?.isFileExist(context) == false) {
                imageUri = null
                ChatLog.e("getImageShowSize", "video file not exist")
            }
        }
    }
    return imageUri
}

/**
 * Get the size of image or video.
 */
fun ChatMessage.getImageSize(context: Context): EaseSize? {
    if (type != ChatMessageType.IMAGE && type != ChatMessageType.VIDEO) {
        return null
    }
    val originalSize = EaseSize(0,0)
    if (type == ChatMessageType.IMAGE) {
        (body as ChatImageMessageBody).let {
            originalSize.width = it.width
            originalSize.height = it.height
        }
    } else if (type == ChatMessageType.VIDEO) {
        (body as ChatVideoMessageBody).let {
            originalSize.width = it.thumbnailWidth
            originalSize.height = it.thumbnailHeight
        }
    }
    val imageUri: Uri? = getThumbnailLocalUri(context)
    // If not has original size, get size from uri.
    if (originalSize.isEmpty() && imageUri != null) {
        try {
            ChatImageUtils.getBitmapOptions(context, imageUri)?.let {
                originalSize.width = it.outWidth
                originalSize.height = it.outHeight
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return originalSize
}

/**
 * Get the show size of image or video according to the screen info.
 */
fun ChatMessage.getImageShowSize(context: Context?): EaseSize? {
    if (context == null) return null
    val originalSize = getImageSize(context) ?: return null
    val maxSize = context.getImageMaxSize()
    // if not get the screen size, the image show the wrap_content size.
    if (maxSize.isEmpty()) {
        return null
    }
    var radio = originalSize.width * 1.0f / if (originalSize.height == 0) 1 else originalSize.height
    if (radio == 0f) {
        radio = 1f
    }
    val showSize = EaseSize(0, 0)
    when(radio) {
        // If radio is less than 1/10, the middle part will be cut off to display the 1/10 part.
        in 0f..1/10f -> {
            showSize.width = (maxSize.height / 10f).toInt()
            showSize.height = maxSize.height
        }
        // If radio is more than 0.1f and less than 3/4f
        in 1/10f..3/4f -> {
             // the maximum show height is used
            showSize.width = (maxSize.height * radio).toInt()
            showSize.height = maxSize.height
        }
        in 3/4f..10f -> {
            // the maximum show width is used
            showSize.width = maxSize.width
            showSize.height = (maxSize.width / radio).toInt()
        }
        else -> {
            showSize.width = maxSize.width
            showSize.height = (maxSize.width / 10f).toInt()
        }
    }
    return showSize
}

/**
 * Check whether the message is a silent message.
 */
internal fun ChatMessage.isSilentMessage(): Boolean {
    return getBooleanAttribute("em_ignore_notification", false)
}

/**
 * Check whether the message can be edited.
 */
internal fun ChatMessage.canEdit(): Boolean {
    return type == ChatMessageType.TXT
            && isSend()
            && isSuccess()
            && EaseIM.getConfig()?.chatConfig?.enableModifyMessageAfterSent == true
}

internal fun ChatMessage.isSuccess(): Boolean {
    return status() == ChatMessageStatus.SUCCESS
}

internal fun ChatMessage.isFail(): Boolean {
    return status() == ChatMessageStatus.FAIL
}

internal fun ChatMessage.inProgress(): Boolean {
    return status() == ChatMessageStatus.INPROGRESS
}

internal fun ChatMessage.isUnsentMessage(): Boolean {
    return if (type == ChatMessageType.TXT) {
        getBooleanAttribute(EaseConstant.MESSAGE_TYPE_RECALL, false)
    } else {
        false
    }
}

internal fun ChatMessage.getInviteMessageStatus(): InviteMessageStatus? {
    if (conversationId() == EaseConstant.DEFAULT_SYSTEM_MESSAGE_ID) {
        return InviteMessageStatus.valueOf(getStringAttribute(EaseConstant.SYSTEM_MESSAGE_STATUS))
    }
    return null
}

internal fun ChatMessage.get(): EaseMessage {
    return EaseMessage.create(this)
}

internal fun ChatMessage.create(): EaseMessage {
    return EaseMessage.create(this, true)
}