package com.hyphenate.easeui.model

import android.text.TextUtils
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.extensions.getInitialLetter
import com.hyphenate.easeui.common.extensions.getUpperFirstWord
import com.hyphenate.easeui.common.extensions.getUserInfo
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.provider.getSyncUser
import java.io.Serializable

/**
 * User bean for easeUI
 * @param userId The user id assigned from app, which should be unique in the application
 * @param nickname The user's nickname, default is userId
 * @param initialLetter The initial letter of user's nickname or userId, it will be used in contact list
 * @param avatar The user's avatar
 * @param contact 0: normal, 1: black ,3: no friend
 * @param email The user's email
 * @param phone The user's phone
 * @param gender The user's gender
 * @param sign The user's signature
 * @param birth The user's birth
 * @param ext The user's ext
 * @param remark The user's remark
 */
data class ChatUIKitUser @JvmOverloads constructor(
    val userId: String,
    var nickname: String? = userId,
    var initialLetter: String? = null,
    var avatar: String? = null,
    var contact: Int = 0,
    var email: String? = null,
    var phone: String? = null,
    var gender: Int = 0,
    var sign: String? = null,
    var birth: String? = null,
    var ext: String? = null,
    var remark: String? = null
) : Serializable{
    /**
     * the timestamp when set initialLetter
     */
    var modifyInitialLetterTimestamp: Long = 0


    /**
     * the timestamp when last modify
     */
    val lastModifyTimestamp: Long = 0

    fun getRemarkOrName(): String? {
        return if (remark.isNullOrEmpty()) getNickname() else remark
    }

    override fun toString(): String {
        return "userId: $userId \n nickname: $nickname \n avatar: $avatar \n remark: $remark"
    }
}

/**
 * Get the user's nickname, if nickname is null or empty, return userId
 */
fun ChatUIKitUser.getNickname(): String? {
    return if (nickname.isNullOrEmpty()) userId else nickname
}

fun ChatUIKitUser.getUserInitialLetter():String?{
    return if (initialLetter.isNullOrEmpty() || lastModifyTimestamp > modifyInitialLetterTimestamp) {
        if (!TextUtils.isEmpty(nickname)) {
            nickname?.let { it.getInitialLetter() }
        } else userId.getInitialLetter()
    } else initialLetter
}

internal fun ChatUIKitUser.setUserInitialLetter(initialLetter: String?){
    this.initialLetter = initialLetter
    modifyInitialLetterTimestamp = System.currentTimeMillis()
}

fun ChatUIKitUser.setUserInitialLetter(){
    var letter = "#"
    val nickname = getRemarkOrName()

    if (nickname.isNullOrEmpty()) {
        setUserInitialLetter(letter)
        return
    }
    nickname?.let {
        setUserInitialLetter(it.getUpperFirstWord())
    } ?: kotlin.run {
        setUserInitialLetter(letter)
    }
}

/**
 * Check whether the user is current user.
 */
fun ChatUIKitUser.isCurrentUser(): Boolean {
    return userId == ChatClient.getInstance().currentUser
}

/**
 * Get the user info from message.
 */
fun ChatUIKitUser.getMessageUser(message: ChatMessage): ChatUIKitUser {
    // get user info from message cache
    var profile = message.getUserInfo()
    if (profile != null && TextUtils.equals(profile.id, userId)) return profile.toUser()
    // get user info from contact provider
    profile = ChatUIKitClient.getUserProvider()?.getSyncUser(userId)
    if (profile != null) return profile.toUser()
    return this
}