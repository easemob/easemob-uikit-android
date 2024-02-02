package com.hyphenate.easeui.model

import java.io.Serializable


/**
 * The class is used to display the group information.
 * @param groupId The group id.
 * @param groupName The group name.
 * @param groupAvatarUrl The group avatarUrl.
 */
data class EaseGroup(
    val groupId: String,
    val groupName:String,
    val groupAvatarUrl:String,
) : Serializable{

}