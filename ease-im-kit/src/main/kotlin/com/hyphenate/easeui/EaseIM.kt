package com.hyphenate.easeui

import android.content.Context
import com.hyphenate.easeui.common.ChatConnectionListener
import com.hyphenate.easeui.common.ChatContactListener
import com.hyphenate.easeui.common.ChatConversationListener
import com.hyphenate.easeui.common.ChatGroupChangeListener
import com.hyphenate.easeui.common.ChatMessageListener
import com.hyphenate.easeui.common.ChatMultiDeviceListener
import com.hyphenate.easeui.common.ChatOptions
import com.hyphenate.easeui.common.ChatPresenceListener
import com.hyphenate.easeui.common.ChatRoomChangeListener
import com.hyphenate.easeui.common.enums.EaseCacheType
import com.hyphenate.easeui.common.impl.EaseIMClientImpl
import com.hyphenate.easeui.common.impl.OnError
import com.hyphenate.easeui.common.impl.OnSuccess
import com.hyphenate.easeui.common.helper.EaseNotifier
import com.hyphenate.easeui.interfaces.EaseIMClient
import com.hyphenate.easeui.interfaces.OnEventResultListener
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.provider.EaseCustomActivityRoute
import com.hyphenate.easeui.provider.EaseConversationInfoProvider
import com.hyphenate.easeui.provider.EaseEmojiconInfoProvider
import com.hyphenate.easeui.provider.EaseGroupMemberProfileProvider
import com.hyphenate.easeui.provider.EaseSettingsProvider
import com.hyphenate.easeui.provider.EaseUserProfileProvider


/**
 * It is the main class of the Chat UIKit.
 */
object EaseIM {
    // Whether the debug mode is open in EaseIM.
    const val DEBUG: Boolean = true

    private val client: EaseIMClient by lazy {
        EaseIMClientImpl()
    }

    /**
     * Initialize the Chat UIKit.
     * @param context The application context.
     * @param options The options of the Chat SDK.
     */
    fun init(context: Context, options: ChatOptions, config: EaseIMConfig? = null): EaseIM {
        client.init(context, options)
        client.setConfig(config)
        return this
    }

    /**
     * Judge whether the uikit is be initialized.
     */
    fun isInited(): Boolean {
        return getContext() != null
    }

    /**
     * Temp for test.
     */
    fun login(userId: String,
              password: String,
              onSuccess: OnSuccess = {},
              onError: OnError = {_,_->}) {
        client.login(userId, password, onSuccess, onError)
    }

    /**
     * Login with user object by token.
     * @param user The user object, see [EaseUser].
     * @param token The token.
     * @param onSuccess The callback of success.
     * @param onError The callback of error.
     */
    fun login(user: EaseProfile,
              token: String,
              onSuccess: OnSuccess = {},
              onError: OnError = {_,_ ->}) {
        client.login(user, token, onSuccess, onError)
    }



    /**
     * Log out from the Chat SDK.
     * @param unbindDeviceToken Whether unbind the device token.
     * @param onSuccess The callback of success.
     * @param onError The callback of error.
     */
    fun logout(unbindDeviceToken: Boolean,
               onSuccess: OnSuccess = {},
               onError: OnError = {_,_ ->}) {
        client.logout(unbindDeviceToken, onSuccess, onError)
    }

    /**
     * Whether the user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return client.isLoggedIn()
    }

    /**
     * Update the current user.
     */
    fun updateCurrentUser(user: EaseProfile) {
        client.updateCurrentUser(user)
    }

    /**
     * Get the current user.
     */
    fun getCurrentUser(): EaseProfile? {
        return client.getCurrentUser()
    }

    /**
     * Set the provider of the emoji icon.
     * @param provider The provider of the emoji icon.
     */
    fun setEmojiconInfoProvider(provider: EaseEmojiconInfoProvider): EaseIM {
        client.setEmojiconInfoProvider(provider)
        return this
    }

    /**
     * Set the conversation information provider.
     * @param provider The provider of the conversation information.
     */
    fun setConversationInfoProvider(provider: EaseConversationInfoProvider): EaseIM {
        client.setConversationInfoProvider(provider)
        return this
    }

    /**
     * Set the userinfo provider.
     * @param provider The provider of the userinfo.
     */
    fun setUserProfileProvider(provider: EaseUserProfileProvider): EaseIM {
        client.setUserProfileProvider(provider)
        return this
    }

    /**
     * Update the UIKit conversation information in cache.
     * @param profiles The profiles to update.
     */
    fun updateConversationInfo(profiles: List<EaseProfile>) {
        client.updateProfiles(profiles)
    }

    /**
     * Update the UIKit user information in cache.
     * @param users The profiles to update.
     */
    fun updateUsersInfo(users: List<EaseProfile>) {
        client.updateUsersInfo(users)
    }

    /**
     * Set the provider of the group member profile.
     * @param provider The provider of the group member profile.
     */
    fun setGroupMemberProfileProvider(provider: EaseGroupMemberProfileProvider): EaseIM {
        client.setGroupMemberProfileProvider(provider)
        return this
    }

    /**
     * Update the UIKit group member profiles in cache.
     * @param groupId The group id.
     */
    fun updateGroupMemberProfiles(groupId: String, profiles: List<EaseProfile>) {
        client.updateGroupMemberProfiles(groupId, profiles)
    }

    /**
     * Set the provider of the settings.
     * @param provider The provider of the settings.
     */
    fun setSettingsProvider(provider: EaseSettingsProvider): EaseIM {
        client.setSettingsProvider(provider)
        return this
    }

    /**
     * Set the activity route in UIKit.
     * @param route The provider of the activity route.
     */
    fun setCustomActivityRoute(route: EaseCustomActivityRoute): EaseIM {
        client.setCustomActivityRoute(route)
        return this
    }

    /**
     * Get the application context.
     */
    fun getContext(): Context? {
        return client.getContext()
    }

    /**
     * Get the UIKit SDK configurations.
     */
    fun getConfig(): EaseIMConfig? {
        return client.getConfig()
    }

    /**
     * Clear the cache.
     */
    fun clearCache(type: EaseCacheType? = EaseCacheType.ALL) {
        client.clearKitCache(type)
    }

    /**
     * Get the cache.
     */
    internal fun getCache() = client.getKitCache()

    /**
     * Get the notification helper in EaseUIKit.
     */
    fun getNotifier(): EaseNotifier? {
        return client.getNotifier()
    }

    /**
     * Get the emojicon provider.
     */
    fun getEmojiconInfoProvider(): EaseEmojiconInfoProvider? {
        return client.getEmojiconInfoProvider()
    }

    /**
     * Get the conversation information provider.
     */
    fun getConversationInfoProvider(): EaseConversationInfoProvider? {
        return client.getConversationInfoProvider()
    }

    /**
     * Get the userinfo provider.
     */
    fun getUserProvider(): EaseUserProfileProvider? {
        return client.getUserProvider()
    }

    /**
     * Get the group member profile provider.
     */
    fun getGroupMemberProfileProvider(): EaseGroupMemberProfileProvider? {
        return client.getGroupMemberProfileProvider()
    }

    /**
     * Get the settings provider.
     */
    fun getSettingsProvider(): EaseSettingsProvider? {
        return client.getSettingsProvider()
    }

    /**
     * Get the activity route provider.
     */
    fun getCustomActivityRoute(): EaseCustomActivityRoute? {
        return client.getCustomActivityRoute()
    }

    /**
     * Add Connection Listener
     */
    fun addConnectionListener(connectListener:ChatConnectionListener){
        client.addConnectionListener(connectListener)
    }

    /**
     * Remove Connection Listener
     */
    fun removeConnectionListener(connectListener:ChatConnectionListener){
        client.removeConnectionListener(connectListener)
    }

    /**
     * Add ChatMessage Listener
     */
    fun addChatMessageListener(listener:ChatMessageListener){
        client.addChatMessageListener(listener)
    }

    /**
     * Remove ChatMessage Listener
     */
    fun removeChatMessageListener(listener:ChatMessageListener){
        client.removeChatMessageListener(listener)
    }

    /**
     * Add GroupChange Listener
     */
    fun addGroupChangeListener(listener:ChatGroupChangeListener){
        client.addGroupChangeListener(listener)
    }

    /**
     * Remove GroupChange Listener
     */
    fun removeGroupChangeListener(listener:ChatGroupChangeListener){
        client.removeGroupChangeListener(listener)
    }

    /**
     * Add Contact Listener
     */
    fun addContactListener(listener:ChatContactListener){
        client.addContactListener(listener)
    }

    /**
     * Remove Contact Listener
     */
    fun removeContactListener(listener:ChatContactListener){
        client.removeContactListener(listener)
    }

    /**
     * Add Conversation Listener
     */
    fun addConversationListener(listener:ChatConversationListener){
        client.addConversationListener(listener)
    }

    /**
     * Remove Conversation Listener
     */
    fun removeConversationListener(listener:ChatConversationListener){
        client.removeConversationListener(listener)
    }

    /**
     * Add Presence Listener
     */
    fun addPresenceListener(listener:ChatPresenceListener){
        client.addPresenceListener(listener)
    }

    /**
     * Remove Presence Listener
     */
    fun removePresenceListener(listener:ChatPresenceListener){
        client.removePresenceListener(listener)
    }

    /**
     * Add ChatRoomChange Listener
     */
    fun addChatRoomChangeListener(listener:ChatRoomChangeListener){
        client.addChatRoomChangeListener(listener)
    }

    /**
     * Remove ChatRoomChange Listener
     */
    fun removeChatRoomChangeListener(listener:ChatRoomChangeListener){
        client.removeChatRoomChangeListener(listener)
    }

    /**
     * Add MultiDevice Listener
     */
    fun addMultiDeviceListener(listener:ChatMultiDeviceListener){
        client.addMultiDeviceListener(listener)
    }

    /**
     * Remove MultiDevice Listener
     */
    fun removeMultiDeviceListener(listener:ChatMultiDeviceListener){
        client.removeMultiDeviceListener(listener)
    }

    /**
     * Add Event Result Listener
     */
    fun addEventResultListener(listener: OnEventResultListener){
        client.addEventResultListener(listener)
    }

    /**
     * Remove Event Result Listener
     */
    fun removeEventResultListener(listener:OnEventResultListener){
        client.removeEventResultListener(listener)
    }

    /**
     * Set Event Result Callback
     */
    fun setEventResultCallback(function: String, errorCode: Int, errorMessage: String?){
        client.callbackEvent(function, errorCode, errorMessage)
    }

}