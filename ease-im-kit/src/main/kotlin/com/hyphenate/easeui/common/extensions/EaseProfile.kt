package com.hyphenate.easeui.common.extensions

import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser

/**
 * Convert [EaseProfile] to [EaseUser].
 */
fun EaseProfile.toUser(): EaseUser {
    return EaseUser(userId = id, nickname = name, avatar = avatar)
}