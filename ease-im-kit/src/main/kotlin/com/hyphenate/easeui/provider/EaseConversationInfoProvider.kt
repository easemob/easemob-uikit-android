package com.hyphenate.easeui.provider

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.impl.OnValueSuccess
import com.hyphenate.easeui.model.EaseProfile
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Conversation info provider.
 */
interface EaseConversationInfoProvider {
    /**
     * return EaseProfile for input conversation id
     * @param id    The conversation id.
     * @param type  The type of conversation.
     * @return
     */
    fun getProfile(id: String?, type: ChatConversationType = ChatConversationType.Chat): EaseProfile?

    /**
     * Fetch profiles from server and callback to UI SDK.
     * @param idsMap  The conversation list stop scrolling, and the visible items which do not have profile will be fetched.
     * @param onValueSuccess The callback of success called by developer.
     */
    fun fetchProfiles(idsMap: Map<ChatConversationType, List<String>>, onValueSuccess: OnValueSuccess<List<EaseProfile>>)
}

/**
 * Suspended function for fetching profiles.
 */
suspend fun EaseConversationInfoProvider.fetchProfilesBySuspend(idsMap: Map<ChatConversationType, List<String>>): List<EaseProfile> {
    return suspendCoroutine { continuation ->
        fetchProfiles(idsMap, onValueSuccess = { map ->
            continuation.resume(map)
        })
    }
}

/**
 * Get profile by cache or sync method provided by developer.
 */
fun EaseConversationInfoProvider.getSyncProfile(id: String?, type: ChatConversationType = ChatConversationType.Chat): EaseProfile? {
    var profile = EaseIM.getCache().getConvInfo(id)
    if (profile == null) {
        profile = getProfile(id, type)
        if (profile != null && !id.isNullOrEmpty()) {
            EaseIM.getCache().insertConvInfo(id, profile)
        }
    }
    return profile
}