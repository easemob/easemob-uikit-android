package com.hyphenate.easeui.provider

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.impl.OnValueSuccess
import com.hyphenate.easeui.model.EaseProfile
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * It is a interface for group member profile provider.
 */
interface EaseGroupMemberProfileProvider {
    /**
     * Get the profile of the member.
     * @param groupId The group id.
     * @param username The username of the member.
     * @return The profile of the member.
     */
    fun getMemberProfile(groupId: String?, username: String?): EaseProfile?

    /**
     * Fetch profiles from server, and callback the result.
     * @param members  KEY: groupId , Value userId list
     * @param onValueSuccess The callback of the result.
     */
    fun fetchMembers(members: MutableMap<String,MutableList<String>>, onValueSuccess: OnValueSuccess<Map<String, EaseProfile>>)
}


/**
 * Suspended function for fetching member profiles.
 */
suspend fun EaseGroupMemberProfileProvider.fetchMemberProfilesBySuspend(members: MutableMap<String,MutableList<String>>): Map<String, EaseProfile> {
    return suspendCoroutine { continuation ->
        fetchMembers(members, onValueSuccess = { map ->
            continuation.resume(map)
        })
    }
}

/**
 * Get member profile by cache or sync method provided by user.
 */
fun EaseGroupMemberProfileProvider.getSyncMemberProfile(groupId:String?,id: String?): EaseProfile? {
    var profile = EaseIM.getCache().getGroupUser(groupId,id)
    if (profile == null) {
        profile = getMemberProfile(groupId,id)
        profile?.let {
            if (!groupId.isNullOrEmpty() && !it.id.isNullOrEmpty()) {
                EaseIM.getCache().insertGroupUser(groupId, profile.toUser())
            }
        }
    }
    return profile
}