package com.hyphenate.easeui.feature.chat.interfaces

import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.interfaces.IControlDataView
import com.hyphenate.easeui.model.EaseMessage
import org.json.JSONObject

interface IHandleChatResultView : IControlDataView {

    /**
     * Callback when ack conversation read successfully.
     */
    fun ackConversationReadSuccess()

    /**
     * Callback when ack conversation read failed.
     */
    fun ackConversationReadFail(code: Int,  message: String?)

    /**
     * Callback when send group read ack successfully.
     */
    fun ackGroupMessageReadSuccess()

    /**
     * Callback when send group read ack failed.
     * @param code
     * @param message
     */
    fun ackGroupMessageReadFail(code: Int,  message: String?)

    /**
     * Callback when send message read ack successfully.
     */
    fun ackMessageReadSuccess()

    /**
     * Callback when send message read ack failed.
     * @param code
     * @param message
     */
    fun ackMessageReadFail(code: Int,  message: String?)

    /**
     * Failed to generate video cover
     * @param message
     */
    fun createThumbFileFail(message: String?)

    /**
     * Before sending a message, add message attributes, such as setting ext, etc.
     * @param message
     */
    fun addMsgAttrBeforeSend(message: ChatMessage?)

    /**
     * Has a error before sending a message.
     * @param code Error code.
     * @param message Error message.
     */
    fun onErrorBeforeSending(code: Int, message: String?)

    /**
     * Delete local message
     * @param message
     */
    fun deleteLocalMessageSuccess(message: EaseMessage?)

    /**
     * Delete local message list successfully.
     */
    fun deleteLocalMessagesSuccess()

    /**
     * Complete withdrawal message
     * @param originalMessage The message was unsent
     * @param notification  The notification message
     */
    fun recallMessageFinish(originalMessage: EaseMessage?, notification: EaseMessage?)

    /**
     * Failed to withdraw the message
     * @param code
     * @param message
     */
    fun recallMessageFail(code: Int, message: String?)

    /**
     * message send success
     * @param message
     */
    fun onSendMessageSuccess(message: EaseMessage?)

    /**
     * message send fail
     * @param message
     * @param code
     * @param error
     */
    fun onSendMessageError(message: EaseMessage?, code: Int, error: String?)

    /**
     * message in sending progress
     * @param message
     * @param progress
     */
    fun onSendMessageInProgress(message: EaseMessage?, progress: Int)

    /**
     * Complete the message sending action
     * @param message
     */
    fun sendMessageFinish(message: EaseMessage?)

    /**
     * add reaction success
     *
     * @param message
     */
    fun addReactionMessageSuccess(message: EaseMessage?)

    /**
     * add reaction fail
     *
     * @param message
     * @param code
     * @param error
     */
    fun addReactionMessageFail(message: EaseMessage?, code: Int, error: String?)

    /**
     * remove reaction success
     *
     * @param message
     */
    fun removeReactionMessageSuccess(message: EaseMessage?)

    /**
     * remove reaction fail
     *
     * @param message
     * @param code
     * @param error
     */
    fun removeReactionMessageFail(message: EaseMessage?, code: Int, error: String?)

    /**
     * modify message success
     * @param messageModified
     */
    fun onModifyMessageSuccess(messageModified: EaseMessage?)

    /**
     * modify message failure
     * @param messageId
     * @param code
     * @param error
     */
    fun onModifyMessageFailure(messageId: String?, code: Int, error: String?)

    /**
     * create reply message ext success.
     * @param extObject reply ext object.
     */
    fun createReplyMessageExtSuccess(extObject: JSONObject?)

    /**
     * create reply message ext fail.
     * @param code  error code.
     * @param error error message.
     */
    fun createReplyMessageExtFail(code: Int, error: String?)

    /**
     * report message success.
     * @param msgId msgId.
     */
    fun reportMessageSuccess(msgId:String){}

    /**
     * report message fail.
     * @param msgId msgId.
     * @param code  error code.
     * @param error error message.
     */
    fun reportMessageFail(msgId: String,code: Int, error: String){}
}