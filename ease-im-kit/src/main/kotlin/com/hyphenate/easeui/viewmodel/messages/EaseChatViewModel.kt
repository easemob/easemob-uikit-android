package com.hyphenate.easeui.viewmodel.messages

import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatCmdMessageBody
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageBody
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatTextMessageBody
import com.hyphenate.easeui.common.ChatType
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.extensions.addUserInfo
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.createUnsentMessage
import com.hyphenate.easeui.common.extensions.get
import com.hyphenate.easeui.common.extensions.isChatroom
import com.hyphenate.easeui.common.extensions.isGroupChat
import com.hyphenate.easeui.common.helper.EaseAtMessageHelper
import com.hyphenate.easeui.common.impl.CallbackImpl
import com.hyphenate.easeui.common.utils.EaseFileUtils
import com.hyphenate.easeui.feature.chat.enums.EaseChatType
import com.hyphenate.easeui.feature.chat.enums.getConversationType
import com.hyphenate.easeui.feature.chat.interfaces.IHandleChatResultView
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.repository.EaseChatManagerRepository
import com.hyphenate.easeui.common.enums.EaseReplyMap
import com.hyphenate.easeui.common.extensions.create
import com.hyphenate.easeui.common.utils.createExpressionMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

open class EaseChatViewModel: ViewModel(), IChatViewRequest {
    
    private var _view: IHandleChatResultView? = null
    private var _conversation: ChatConversation? = null
    private val chatRepository by lazy { EaseChatManagerRepository() }

    override fun attachView(view: IControlDataView) {
        _view = view as IHandleChatResultView
    }
    override fun setupWithToUser(
        toChatUsername: String?,
        chatType: EaseChatType?,
        isThread: Boolean
    ) {
        _conversation = ChatClient.getInstance().chatManager().getConversation(toChatUsername
            , chatType?.getConversationType(), true, isThread)
    }

    override fun sendChannelAck() {
        safeInConvScope {
            viewModelScope.launch {
                flow {
                    emit(chatRepository.ackConversationRead(it.conversationId()))
                }
                .catchChatException { e ->
                    _view?.ackConversationReadFail(e.errorCode, e.description)
                }
                .collect {
                    _view?.ackConversationReadSuccess()
                }
            }

        }
    }

    override fun sendGroupMessageReadAck(messageId: String?, ext: String?) {
        safeInConvScope {
            viewModelScope.launch {
                flow {
                    emit(chatRepository.ackGroupMessageRead(it.conversationId(), messageId, ext))
                }
                .catchChatException { e ->
                    _view?.ackGroupMessageReadFail(e.errorCode, e.description)
                }
                .collect {
                    _view?.ackGroupMessageReadSuccess()
                }
            }
        }
    }

    override fun sendMessageReadAck(messageId: String?) {
        safeInConvScope {
            viewModelScope.launch {
                flow {
                    emit(chatRepository.ackMessageRead(it.conversationId(), messageId))
                }
                .catchChatException { e ->
                    _view?.ackMessageReadFail(e.errorCode, e.description)
                }
                .collect {
                    _view?.ackMessageReadSuccess()
                }
            }
        }
    }

    override fun sendTextMessage(content: String?, isNeedGroupAck: Boolean) {
        safeInConvScope {
            if (it.isGroupChat) {
                if (EaseAtMessageHelper.get().containsAtUsername(content)) {
                    sendAtMessage(content)
                    return@safeInConvScope
                }
            }
            val message = ChatMessage.createTextSendMessage(content, it.conversationId())
            if (it.isGroupChat) {
                message.setIsNeedGroupAck(isNeedGroupAck)
            }
            sendMessage(message)
        }
    }

    override fun sendAtMessage(content: String?) {
        safeInConvScope {
            if (!it.isGroupChat) {
                inMainScope {
                    _view?.onErrorBeforeSending(ChatError.INVALID_PARAM, "Not group chat.")
                }
                return@safeInConvScope
            }
            val group: ChatGroup? = ChatClient.getInstance().groupManager().getGroup(it.conversationId())
            if (group == null) {
                inMainScope {
                    _view?.onErrorBeforeSending(ChatError.INVALID_PARAM, "Group: ${it.conversationId()} is null.")
                }
                return@safeInConvScope
            }
            val message = ChatMessage.createTextSendMessage(content, it.conversationId())
            if (ChatClient.getInstance().currentUser == group.owner
                && EaseAtMessageHelper.get().containsAtAll(content)) {
                message.setAttribute(EaseConstant.MESSAGE_ATTR_AT_MSG, EaseConstant.MESSAGE_ATTR_VALUE_AT_MSG_ALL)
            } else {
                message.setAttribute(
                    EaseConstant.MESSAGE_ATTR_AT_MSG,
                    EaseAtMessageHelper.get().atListToJsonArray(
                        EaseAtMessageHelper.get().getAtMessageUsernames(content!!)
                    )
                )
            }
            sendMessage(message)
        }
    }

    override fun sendBigExpressionMessage(name: String?, identityCode: String?) {
        if (_conversation == null) {
            inMainScope {
                _view?.onErrorBeforeSending(ChatError.INVALID_PARAM, "Conversation is null.")
            }
            return
        }
        _conversation?.let {
            val message: ChatMessage? = createExpressionMessage(it.conversationId(), name, identityCode)
            sendMessage(message)
        }
    }

    override fun sendVoiceMessage(filePath: Uri?, length: Int) {
        safeInConvScope {
            val message = ChatMessage.createVoiceSendMessage(filePath, length, it.conversationId())
            sendMessage(message)
        }
    }

    override fun sendImageMessage(imageUri: Uri?, sendOriginalImage: Boolean) {
        safeInConvScope {
            val message =
                ChatMessage.createImageSendMessage(imageUri, sendOriginalImage, it.conversationId())
            sendMessage(message)
        }
    }

    override fun sendLocationMessage(
        latitude: Double,
        longitude: Double,
        locationAddress: String?
    ) {
        safeInConvScope {
            val message = ChatMessage.createLocationSendMessage(
                latitude,
                longitude,
                locationAddress,
                it.conversationId()
            )
            ChatLog.i(
                TAG,
                "current = " + ChatClient.getInstance().currentUser + " to = " + it.conversationId()
            )
            sendMessage(message)
        }
    }

    override fun sendVideoMessage(videoUri: Uri?, videoLength: Int) {
        safeInConvScope {
            val thumbPath: String = EaseFileUtils.getThumbPath(EaseIM.getContext(), videoUri)
            val message = ChatMessage.createVideoSendMessage(
                videoUri,
                thumbPath,
                videoLength,
                it.conversationId()
            )
            sendMessage(message)
        }
    }

    override fun sendFileMessage(fileUri: Uri?) {
        safeInConvScope {
            val message = ChatMessage.createFileSendMessage(fileUri, it.conversationId())
            sendMessage(message)
        }
    }

    override fun sendCombineMessage(
        title: String?,
        summary: String?,
        compatibleText: String?,
        msgIds: List<String?>?
    ) {
        safeInConvScope {
            val message = ChatMessage.createCombinedSendMessage(
                title,
                summary,
                compatibleText,
                msgIds,
                it.conversationId()
            )
            sendMessage(message)
        }
    }

    override fun sendCombineMessage(message: ChatMessage?) {
        safeInConvScope {
            sendMessage(message, false)
        }
    }

    override fun sendCmdMessage(action: String?) {
        safeInConvScope {
            val beginMsg = ChatMessage.createSendMessage(ChatMessageType.CMD)
            val body = ChatCmdMessageBody(action)
            // Only deliver this cmd msg to online users
            body.deliverOnlineOnly(true)
            beginMsg.addBody(body)
            beginMsg.to = it.conversationId()
            ChatClient.getInstance().chatManager().sendMessage(beginMsg)
        }
    }

    override fun addMessageAttributes(message: ChatMessage?) {
        _view?.addMsgAttrBeforeSend(message)
    }

    override fun sendMessage(message: ChatMessage?) {
        safeInConvScope {
            sendMessage(message, true)
        }
    }

    override fun sendMessage(message: ChatMessage?, isCheck: Boolean) {
        safeInConvScope {
            if (message == null) {
                inMainScope {
                    _view?.onErrorBeforeSending(ChatError.MESSAGE_INVALID, "Message is null.")
                    return@inMainScope
                }
            }
            message?.run {
                if (isCheck) {
                    if (it.isGroupChat) {
                        chatType = ChatType.GroupChat
                    } else if (it.isChatroom) {
                        chatType = ChatType.ChatRoom
                    }
                    setIsChatThreadMessage(it.isChatThread)
                }
                EaseIM.getCurrentUser()?.let {
                    addUserInfo(it.name, it.avatar)
                }
                addMessageAttributes(message)
                val msg = message.get()
                msg.setMessageStatusCallback(CallbackImpl(
                    onSuccess = {
                        inMainScope {
                            _view?.onSendMessageSuccess(msg)
                        }
                    },
                    onError = { code, error ->
                        inMainScope {
                            _view?.onSendMessageError(msg, code, error)
                        }
                    },
                    onProgress = {
                        inMainScope {
                            _view?.onSendMessageInProgress(msg, it)
                        }
                    }
                ), true)
                ChatClient.getInstance().chatManager().sendMessage(message)
                inMainScope {
                    _view?.sendMessageFinish(msg)
                }
            }
        }
    }

    override fun resendMessage(message: EaseMessage?) {
        safeInConvScope {
            message?.getMessage()?.let {
                it.setStatus(ChatMessageStatus.CREATE)
                val currentTimeMillis = System.currentTimeMillis()
                it.setLocalTime(currentTimeMillis)
                it.msgTime = currentTimeMillis
                ChatClient.getInstance().chatManager().updateMessage(it)
                sendMessage(it)
            }
        }
    }

    override fun reportMessage(tag: String, reason: String?, msgId: String) {
        safeInConvScope {
            viewModelScope.launch {
                flow {
                    emit(chatRepository.reportMessage(tag, reason,msgId ))
                }
                .catchChatException { e ->
                    _view?.reportMessageFail(msgId,e.errorCode, e.description)
                }
                .collect {
                    _view?.reportMessageSuccess(msgId)
                }
            }
        }
    }

    override fun deleteMessage(message: EaseMessage?) {
        safeInConvScope {
            it.removeMessage(message?.getMessage()?.msgId)
            inMainScope {
                _view?.deleteLocalMessageSuccess(message)
            }
        }
    }

    override fun deleteMessages(messages: List<String?>?) {
        safeInConvScope {
            if (messages.isNullOrEmpty()) return@safeInConvScope
            messages.forEach { item ->
                it.removeMessage(item)
            }
            inMainScope {
                _view?.deleteLocalMessagesSuccess()
            }
        }
    }

    override fun recallMessage(message: EaseMessage?) {
        safeInConvScope { conv->
            message?.getMessage()?.let { msg ->
                viewModelScope.launch {
                    val msgNotification = msg.createUnsentMessage()

                    flow {
                        emit(chatRepository.recallMessage(msg))
                    }
                    .catchChatException { e ->
                        _view?.recallMessageFail(e.errorCode, e.description)
                    }
                    .collect {
                        conv.insertMessage(msgNotification)
                        _view?.recallMessageFinish(message, msgNotification.create())
                    }
                }

            }
        }
    }

    override fun modifyMessage(messageId: String?, messageBodyModified: ChatMessageBody?) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.modifyMessage(messageId, messageBodyModified))
            }
            .catchChatException { e ->
                _view?.onModifyMessageFailure(messageId, e.errorCode, e.description)
            }
            .collect {
                _view?.onModifyMessageSuccess(it.get())
            }
        }
    }

    override fun addReaction(message: EaseMessage?, reaction: String?) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.addReaction(message?.getMessage(), reaction))
            }
            .catchChatException { e->
                _view?.addReactionMessageFail(message, e.errorCode, e.description)
            }
            .collect {
                _view?.addReactionMessageSuccess(message)
            }
        }
    }

    override fun removeReaction(message: EaseMessage?, reaction: String?) {
        viewModelScope.launch {
            flow {
                emit(chatRepository.removeReaction(message?.getMessage(), reaction))
            }
            .catchChatException { e->
                _view?.addReactionMessageFail(message, e.errorCode, e.description)
            }
            .collect {
                _view?.addReactionMessageSuccess(message)
            }
        }
    }

    override fun createReplyMessageExt(message: ChatMessage?) {
        message?.run {
            val quoteObject = JSONObject()
            try {
                if (body != null) {
                    quoteObject.put(EaseConstant.QUOTE_MSG_ID, msgId)
                    if (type === ChatMessageType.TXT && !TextUtils.isEmpty((body as ChatTextMessageBody).message)) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            (body as ChatTextMessageBody).message
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.txt.name)
                    } else if (type === ChatMessageType.IMAGE) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_picture)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.img.name)
                    } else if (type === ChatMessageType.VIDEO) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_video)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.video.name)
                    } else if (type === ChatMessageType.LOCATION) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_location)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.location.name)
                    } else if (type === ChatMessageType.VOICE) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_voice)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.audio.name)
                    } else if (type === ChatMessageType.FILE) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_file)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.file.name)
                    } else if (type === ChatMessageType.CUSTOM) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_custom)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.custom.name)
                    } else if (type === ChatMessageType.COMBINE) {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            EaseIM.getContext()?.getResources()?.getString(R.string.ease_combine)
                        )
                        quoteObject.put(EaseConstant.QUOTE_MSG_TYPE, EaseReplyMap.combine.name)
                    } else {
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_PREVIEW,
                            "[" + type.name.lowercase(Locale.getDefault()) + "]"
                        )
                        quoteObject.put(
                            EaseConstant.QUOTE_MSG_TYPE,
                            type.name.lowercase(Locale.getDefault())
                        )
                    }
                    quoteObject.put(EaseConstant.QUOTE_MSG_SENDER, getFrom())
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                _view?.createReplyMessageExtFail(ChatError.GENERAL_ERROR, e.message)
                return
            }
            _view?.createReplyMessageExtSuccess(quoteObject)
        }
    }

    private fun safeInConvScope(scope: (ChatConversation)->Unit) {
        if (_conversation == null) {
            inMainScope {
                _view?.onErrorBeforeSending(ChatError.INVALID_PARAM, "Conversation is null.")
            }
            return
        }
        _conversation?.let {
            scope(it)
        }
    }

    private fun inMainScope(scope: ()->Unit) {
        viewModelScope.launch(context = Dispatchers.Main) {
            scope()
        }
    }

    companion object {
        private val TAG = EaseChatViewModel::class.java.simpleName
    }
}