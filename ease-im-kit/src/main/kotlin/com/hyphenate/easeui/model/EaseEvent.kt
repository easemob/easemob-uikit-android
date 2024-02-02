package com.hyphenate.easeui.model

import java.io.Serializable

/**
 * Event class.
 */
class EaseEvent @JvmOverloads constructor(
    var event: String?,
    var type: TYPE,
    var message: String? = null,
    var refresh: Boolean = true
) : Serializable {

    val isMessageChange: Boolean
        get() = type == TYPE.MESSAGE
    val isGroupChange: Boolean
        get() = type == TYPE.GROUP
    val isContactChange: Boolean
        get() = type == TYPE.CONTACT
    val isNotifyChange: Boolean
        get() = type == TYPE.NOTIFY
    val isAccountChange: Boolean
        get() = type == TYPE.ACCOUNT
    val isConversationChange: Boolean
        get() = type == TYPE.CONVERSATION

    enum class TYPE {
        /**
         * Group event type.
         */
        GROUP,

        /**
         * Contact event type.
         */
        CONTACT,

        /**
         * Message event type.
         */
        MESSAGE,

        /**
         * Conversation event type.
         */
        CONVERSATION,

        /**
         * Notify event type.
         */
        NOTIFY,

        /**
         * Chat room event type.
         */
        CHAT_ROOM,

        /**
         * Account event type. For example, user logout.
         */
        ACCOUNT
    }

    enum class EVENT {
        LEAVE,
        DESTROY,
        ADD,
        REMOVE,
        UPDATE,
        LOGOUT
    }

}