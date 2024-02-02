package com.hyphenate.easeui.common

import com.hyphenate.easeui.common.enums.EaseCacheType
import com.hyphenate.easeui.common.extensions.toProfile
import com.hyphenate.easeui.common.helper.EasePreferenceManager
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class EaseIMCache {
    private val userMap: ConcurrentMap<String, EaseProfile> = ConcurrentHashMap()
    private val groupMap: ConcurrentMap<String, ConcurrentMap<String,EaseProfile>> = ConcurrentHashMap()
    private val convMap: ConcurrentMap<String, EaseProfile> = ConcurrentHashMap()
    // Cache the userinfo parsed by message ext. The key is the userId, the value is the userinfo.
    private val messageUserMap: ConcurrentMap<String, EaseProfile> = ConcurrentHashMap()
    private val mutedConvMap: MutableMap<String, Long> = HashMap()

    fun insertUser(user: EaseProfile) {
        userMap[user.id] = user
    }

    fun insertGroupUser(groupId:String, user: EaseUser){
        groupMap[groupId] = ConcurrentHashMap<String, EaseProfile>().apply {
            put(user.userId, user.toProfile())
        }
    }

    fun insertConvInfo(conversationId: String, profile: EaseProfile) {
        convMap[conversationId] = profile
    }

    fun getGroupMemberList(groupId:String): MutableList<EaseProfile>{
        return groupMap[groupId]?.map {
            it.value
        }?.toMutableList() ?: mutableListOf()
    }

    fun getUser(userId: String?): EaseProfile? {
        if (userId.isNullOrEmpty()) {
            return null
        }
        return userMap[userId]
    }

    fun getConvInfo(conversationId: String?): EaseProfile? {
        if (conversationId.isNullOrEmpty()) {
            return null
        }
        return convMap[conversationId]
    }

    fun getGroupUser(groupId:String?,userId: String?):EaseProfile?{
        if (groupId.isNullOrEmpty() || userId.isNullOrEmpty() || !groupMap.containsKey(groupId)){
            return null
        }
        return groupMap[groupId]?.get(userId)
    }

    /**
     * Insert message userinfo to cache.
     */
    @Synchronized
    fun insertMessageUser(userId: String, profile: EaseProfile) {
        if (messageUserMap.containsKey(userId)) {
            if (messageUserMap[userId]!!.getTimestamp() < profile.getTimestamp()) {
                return
            }
        }
        messageUserMap[userId] = profile
    }

    /**
     * Get userinfo cache by userId.
     */
    fun getMessageUserInfo(userId: String?): EaseProfile? {
        if (userId.isNullOrEmpty() || !messageUserMap.containsKey(userId)) return null
        return messageUserMap[userId]
    }

    /**
     * Get the muted conversation list.
     */
    @Synchronized
    fun getMutedConversationList(): MutableMap<String, Long> {
        if (mutedConvMap.isNullOrEmpty()) {
            val savedMap = EasePreferenceManager.getInstance().getMuteMap(ChatClient.getInstance().currentUser)
            if (!savedMap.isNullOrEmpty()) {
                mutedConvMap.putAll(savedMap)
            }
            return mutedConvMap
        }
        return mutedConvMap
    }

    /**
     * Add target conversation to mute map.
     */
    @Synchronized
    fun setMutedConversation(conversationId: String, mutedTime: Long = 0) {
        if (mutedConvMap.isNullOrEmpty()) {
            val exist = EasePreferenceManager.getInstance().getMuteMap(ChatClient.getInstance().currentUser)
            if (exist.isNotEmpty()) {
                mutedConvMap.putAll(exist)
            }
        }
        mutedConvMap[conversationId] = mutedTime
        EasePreferenceManager.getInstance().setMuteMap(ChatClient.getInstance().currentUser, mutedConvMap)
    }

    /**
     * Remove target conversation from mute map.
     */
    @Synchronized
    fun removeMutedConversation(conversationId: String) {
        if (mutedConvMap.isNullOrEmpty()) {
            val exist = EasePreferenceManager.getInstance().getMuteMap(ChatClient.getInstance().currentUser)
            if (exist.isNotEmpty()) {
                mutedConvMap.putAll(exist)
            }
        }
        mutedConvMap.remove(conversationId)
        EasePreferenceManager.getInstance().setMuteMap(ChatClient.getInstance().currentUser, mutedConvMap)
    }

    fun clear(type: EaseCacheType?) {
        if (type == null || type == EaseCacheType.ALL) {
            userMap.clear()
            convMap.clear()
            groupMap.clear()
            messageUserMap.clear()
            mutedConvMap.clear()
        } else {
            when (type) {
                EaseCacheType.CONTACT -> userMap.clear()
                EaseCacheType.CONVERSATION_INFO -> convMap.clear()
                EaseCacheType.GROUP_MEMBER -> groupMap.clear()
                else -> {
                }
            }
        }
    }

    fun updateProfiles(profiles: List<EaseProfile>) {
        if (profiles.isNotEmpty()) {
            profiles.forEach {
                convMap[it.id] = it
            }
        }
    }

    fun updateUsers(users: List<EaseProfile>) {
        if (users.isNotEmpty()) {
            users.forEach {
                userMap[it.id] = it
            }
        }
    }

    fun updateGroupMemberProfiles(groupId: String, profiles: List<EaseProfile>) {
        if (profiles.isNotEmpty()) {
            val groupUserMap = groupMap[groupId] ?: ConcurrentHashMap()
            profiles.forEach {
                groupUserMap[it.id] = it
            }
            groupMap[groupId] = groupUserMap
        }
    }
}