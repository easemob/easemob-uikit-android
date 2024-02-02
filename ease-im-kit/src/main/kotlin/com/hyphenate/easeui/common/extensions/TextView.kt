package com.hyphenate.easeui.common.extensions

import android.widget.TextView
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.model.EaseProfile

/**
 * Set nickname from EaseProfile.
 * @param profile
 */
internal fun TextView.loadNickname(profile: EaseProfile?) {
    if (profile == null) return
    text = if (profile.name.isNullOrEmpty()) profile.id else profile.name
}

/**
 * Set nickname from message ext.
 */
internal fun TextView.loadNickname(message: ChatMessage?) {
    if (message == null) return
    loadNickname(message.getUserInfo())
}

internal fun TextView.getTextHeight(): Int {
    return paint.fontMetricsInt.bottom - paint.fontMetricsInt.top
}