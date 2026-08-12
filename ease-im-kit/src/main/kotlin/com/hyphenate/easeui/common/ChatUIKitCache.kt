package com.hyphenate.easeui.common

import com.hyphenate.easeui.common.enums.ChatUIKitCacheType
import com.hyphenate.easeui.common.helper.ChatUIKitPreferenceManager
import com.hyphenate.easeui.model.ChatUIKitGroupProfile
import com.hyphenate.easeui.model.ChatUIKitPreview
import com.hyphenate.easeui.model.ChatUIKitProfile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal data class ChatUIKitGroupNameCard(
    val nameCard: String?,
    val timestamp: Long,
)

private data class GroupMemberKey(
    val groupId: String,
    val userId: String,
)

class ChatUIKitCache {
    // Cache profiles supplied by the app or its user profile provider.
    private val userMap: ConcurrentMap<String, ChatUIKitProfile> = ConcurrentHashMap()
    // Cache profiles synchronized by the SDK. App supplied fields take priority when reading.
    private val sdkUserMap: ConcurrentMap<String, ChatUIKitProfile> = ConcurrentHashMap()
    // Cache the group info. The key is the groupId, the value is the group info.
    private val groupMap: ConcurrentMap<String, ChatUIKitGroupProfile> = ConcurrentHashMap()
    private val groupNameCardMap =
        ConcurrentHashMap<GroupMemberKey, ChatUIKitGroupNameCard>()
    // Cache the userinfo parsed by message ext. The key is the userId, the value is the userinfo.
    private val messageUserMap: ConcurrentMap<String, ChatUIKitProfile> = ConcurrentHashMap()
    private val mutedConvMap: MutableMap<String, Long> = HashMap()
    private val previewMap:ConcurrentMap<String, ChatUIKitPreview> = ConcurrentHashMap()
    private val checkPreviewMap:MutableMap<String,Boolean> = mutableMapOf()

    companion object {
        private const val TAG = "ChatUIKitCache"
    }

    fun init() {
        clear(ChatUIKitCacheType.ALL)
        // Load the muted conversation list from the local storage.
        val muteMap = ChatUIKitPreferenceManager.getInstance().getMuteMap(ChatClient.getInstance().currentUser)
        if (muteMap.isNotEmpty()) {
            mutedConvMap.putAll(muteMap)
        }
    }

    fun insertUser(user: ChatUIKitProfile) {
        userMap[user.id] = user
    }

    internal fun getProviderUser(userId: String?): ChatUIKitProfile? {
        if (userId.isNullOrEmpty()) return null
        return userMap[userId]
    }

    internal fun getSdkUser(userId: String?): ChatUIKitProfile? {
        if (userId.isNullOrEmpty()) return null
        return sdkUserMap[userId]
    }

    @Synchronized
    internal fun insertSdkUser(user: ChatUIKitProfile) {
        val cached = sdkUserMap[user.id]
        sdkUserMap[user.id] = mergeProfiles(user.id, user, cached)
    }

    internal fun updateSdkUsers(users: List<ChatUIKitProfile>) {
        users.forEach(::insertSdkUser)
    }

    /**
     * Insert or update the group info to the cache.
     * @param groupId The group id.
     * @param profile The group info.
     */
    fun insertGroup(groupId: String?, profile: ChatUIKitGroupProfile?) {
        if (groupId.isNullOrEmpty()) {
            ChatLog.e(TAG, "insertGroup: groupId is null or empty")
            return
        }
        groupMap[groupId] = profile
    }

    fun getUser(userId: String?): ChatUIKitProfile? {
        if (userId.isNullOrEmpty()) {
            return null
        }
        return mergeProfiles(userId, userMap[userId], sdkUserMap[userId])
    }

    @Synchronized
    internal fun insertGroupNameCard(
        groupId: String?,
        userId: String?,
        nameCard: String?,
        timestamp: Long,
    ) {
        if (groupId.isNullOrEmpty() || userId.isNullOrEmpty()) return
        val key = GroupMemberKey(groupId, userId)
        val cached = groupNameCardMap[key]
        if (cached == null || timestamp >= cached.timestamp) {
            groupNameCardMap[key] = ChatUIKitGroupNameCard(nameCard, timestamp)
        }
    }

    internal fun getGroupNameCard(groupId: String?, userId: String?): ChatUIKitGroupNameCard? {
        if (groupId.isNullOrEmpty() || userId.isNullOrEmpty()) return null
        return groupNameCardMap[GroupMemberKey(groupId, userId)]
    }

    /**
     * Get the group info by groupId.
     * @param groupId The group id.
     * @return The group info.
     */
    fun getGroup(groupId: String?): ChatUIKitGroupProfile? {
        if (groupId.isNullOrEmpty()) {
            return null
        }
        return groupMap[groupId]
    }

    /**
     * Insert message userinfo to cache.
     */
    @Synchronized
    fun insertMessageUser(userId: String, profile: ChatUIKitProfile) {
        val cached = messageUserMap[userId]
        if (cached != null && cached.getTimestamp() > profile.getTimestamp()) {
            return
        }
        messageUserMap[userId] = profile
    }

    /**
     * Get userinfo cache by userId.
     */
    fun getMessageUserInfo(userId: String?): ChatUIKitProfile? {
        if (userId.isNullOrEmpty() || !messageUserMap.containsKey(userId)) return null
        return messageUserMap[userId]
    }

    /**
     * Get the muted conversation list.
     */
    @Synchronized
    fun getMutedConversationList(): MutableMap<String, Long> {
        return mutedConvMap
    }

    /**
     * Add target conversation to mute map.
     */
    @Synchronized
    fun setMutedConversation(conversationId: String, mutedTime: Long = 0) {
        mutedConvMap[conversationId] = mutedTime
        ChatUIKitPreferenceManager.getInstance().setMuteMap(ChatClient.getInstance().currentUser, mutedConvMap)
    }

    /**
     * Remove target conversation from mute map.
     */
    @Synchronized
    fun removeMutedConversation(conversationId: String) {
        mutedConvMap.remove(conversationId)
        ChatUIKitPreferenceManager.getInstance().setMuteMap(ChatClient.getInstance().currentUser, mutedConvMap)
    }

    fun clear(type: ChatUIKitCacheType?) {
        if (type == null || type == ChatUIKitCacheType.ALL) {
            userMap.clear()
            sdkUserMap.clear()
            groupMap.clear()
            groupNameCardMap.clear()
            messageUserMap.clear()
            mutedConvMap.clear()
            previewMap.clear()
        } else {
            when (type) {
                ChatUIKitCacheType.CONTACT -> {
                    userMap.clear()
                    sdkUserMap.clear()
                }
                ChatUIKitCacheType.CONVERSATION_INFO -> {
                    groupMap.clear()
                    groupNameCardMap.clear()
                }
                else -> {
                }
            }
        }
    }

    fun updateProfiles(profiles: List<ChatUIKitGroupProfile>) {
        if (profiles.isNotEmpty()) {
            profiles.forEach {
                groupMap[it.id] = it
            }
        }
    }

    fun updateUsers(users: List<ChatUIKitProfile>) {
        if (users.isNotEmpty()) {
            users.forEach {
                userMap[it.id] = it
            }
        }
    }

    private fun mergeProfiles(
        userId: String,
        preferred: ChatUIKitProfile?,
        fallback: ChatUIKitProfile?,
    ): ChatUIKitProfile? {
        if (preferred == null && fallback == null) return null
        return ChatUIKitProfile(
            id = userId,
            name = preferred?.name.nonEmptyOrNull() ?: fallback?.name,
            avatar = preferred?.avatar.nonEmptyOrNull() ?: fallback?.avatar,
            remark = preferred?.remark.nonEmptyOrNull() ?: fallback?.remark,
        ).apply {
            setTimestamp(maxOf(preferred?.getTimestamp() ?: 0L, fallback?.getTimestamp() ?: 0L))
        }
    }

    private fun String?.nonEmptyOrNull(): String? = this?.takeIf { it.isNotEmpty() }

    fun saveUrlPreviewInfo(msgId:String?,bean:ChatUIKitPreview){
        msgId?.let {
            if (it.isNotEmpty()){
                previewMap[msgId] = bean
            }
        }
    }

    fun getUrlPreviewInfo(msgId: String?): ChatUIKitPreview? {
        msgId?.let {
            if (previewMap.size > 0 && it.isNotEmpty()) {
                if (previewMap.containsKey(msgId)) {
                    return previewMap[msgId]
                }
            }
        }
        return null
    }

    fun cleanUrlPreviewInfo(msgId: String?){
        msgId?.let {
            if (previewMap.containsKey(it)) {
                previewMap.remove(it)
            }
            if (checkPreviewMap.containsKey(it)){
                checkPreviewMap.remove(it)
            }
        }
    }

    fun checkUrlPreview(msgId:String?,isFirst:Boolean? = true){
        msgId?.let {
            if (it.isNotEmpty()){
                checkPreviewMap[msgId] = isFirst?:true
            }
        }
    }

    fun isFirstLoadedUrlPreview(msgId:String?):Boolean{
        msgId?.let {
            if (checkPreviewMap.isNotEmpty() && it.isNotEmpty()) {
                if (checkPreviewMap.containsKey(msgId)) {
                    return checkPreviewMap[msgId]?:true
                }
            }
        }
        return true
    }

}
