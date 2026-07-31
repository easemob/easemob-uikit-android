package com.hyphenate.easeui.model

import com.hyphenate.chat.EMOptions.EMDataSyncType
import java.io.Serializable

/**
 * Describes the state of an automatic SDK data synchronization task.
 *
 * Subscribe with [eventKey] and `ChatUIKitFlowBus.withStick` to observe the
 * latest state of a specific data type.
 */
class ChatUIKitDataSyncEvent @JvmOverloads constructor(
    val dataSyncType: EMDataSyncType,
    val state: STATE,
    val errorCode: Int? = null,
) : Serializable {

    enum class STATE {
        STARTED,
        FINISHED,
    }

    companion object {
        private const val EVENT_KEY_PREFIX = "DATA_SYNC"

        /**
         * Returns the independent FlowBus key for the specified sync type.
         */
        @JvmStatic
        fun eventKey(dataSyncType: EMDataSyncType): String =
            "$EVENT_KEY_PREFIX/${dataSyncType.name}"
    }
}

internal fun EMDataSyncType?.toUIKitEventType(): ChatUIKitEvent.TYPE? = when (this) {
    EMDataSyncType.CONVERSATIONS -> ChatUIKitEvent.TYPE.CONVERSATION
    EMDataSyncType.CONTACTS -> ChatUIKitEvent.TYPE.CONTACT
    EMDataSyncType.JOINED_GROUPS -> ChatUIKitEvent.TYPE.GROUP
    else -> null
}
