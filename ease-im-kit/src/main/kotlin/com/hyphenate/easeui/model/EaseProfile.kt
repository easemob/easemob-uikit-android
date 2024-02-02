package com.hyphenate.easeui.model


/**
 * It is a bean for profile provider interface.
 * @param id The id, it can be the userId or the group id.
 * @param name The name of the user or the group.
 * @param avatar The avatar of the user or the group.
 */
data class EaseProfile(
    val id: String,
    var name: String? = null,
    val avatar: String? = null
) {
    private var _timestamp: Long = 0L

    internal fun setTimestamp(timestamp: Long) {
        _timestamp = timestamp
    }

    internal fun getTimestamp(): Long {
        return _timestamp
    }
}