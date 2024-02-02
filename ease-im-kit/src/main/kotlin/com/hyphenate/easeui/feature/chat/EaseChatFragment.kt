package com.hyphenate.easeui.feature.chat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatMultiDeviceListener
import com.hyphenate.easeui.common.ChatMultiDeviceListener.GROUP_DESTROY
import com.hyphenate.easeui.common.ChatMultiDeviceListener.GROUP_LEAVE
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseFragment
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.enums.EaseChatFinishReason
import com.hyphenate.easeui.common.extensions.showToast
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.configs.setAvatarStyle
import com.hyphenate.easeui.databinding.EaseFragmentChatBinding
import com.hyphenate.easeui.feature.contact.EaseContactDetailsActivity
import com.hyphenate.easeui.feature.group.EaseGroupDetailActivity
import com.hyphenate.easeui.feature.chat.adapter.EaseMessagesAdapter
import com.hyphenate.easeui.feature.chat.controllers.EaseChatAttachmentController
import com.hyphenate.easeui.feature.chat.controllers.EaseChatMentionController
import com.hyphenate.easeui.feature.chat.enums.EaseChatType
import com.hyphenate.easeui.feature.chat.enums.EaseLoadDataType
import com.hyphenate.easeui.feature.chat.enums.getConversationType
import com.hyphenate.easeui.feature.chat.interfaces.OnChatExtendMenuItemClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnChatFinishListener
import com.hyphenate.easeui.feature.chat.interfaces.OnChatInputChangeListener
import com.hyphenate.easeui.feature.chat.interfaces.OnChatLayoutListener
import com.hyphenate.easeui.feature.chat.interfaces.OnChatRecordTouchListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageItemClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageSendCallBack
import com.hyphenate.easeui.feature.chat.interfaces.OnModifyMessageListener
import com.hyphenate.easeui.feature.chat.interfaces.OnPeerTypingListener
import com.hyphenate.easeui.feature.chat.interfaces.OnReactionMessageListener
import com.hyphenate.easeui.feature.chat.interfaces.OnReportMessageListener
import com.hyphenate.easeui.feature.chat.interfaces.OnWillSendMessageListener
import com.hyphenate.easeui.feature.chat.widgets.EaseChatMessageListLayout
import com.hyphenate.easeui.feature.chat.widgets.EaseInputMenuStyle
import com.hyphenate.easeui.interfaces.OnMenuChangeListener
import com.hyphenate.easeui.menu.chat.EaseChatExtendMenuDialog
import com.hyphenate.easeui.menu.chat.EaseChatMenuHelper
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.model.EaseMenuItem
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.provider.getSyncProfile
import com.hyphenate.easeui.provider.getSyncUser

open class EaseChatFragment: EaseBaseFragment<EaseFragmentChatBinding>(), OnChatLayoutListener,
    OnMenuChangeListener, OnWillSendMessageListener, OnModifyMessageListener,
    ChatMultiDeviceListener, OnReportMessageListener, OnChatFinishListener {
    private var backPressListener: View.OnClickListener? = null
    private var extendMenuItemClickListener: OnChatExtendMenuItemClickListener? = null
    private var chatInputChangeListener: OnChatInputChangeListener? = null
    private var chatItemClickListener: OnMessageItemClickListener? = null
    private var messageSendCallBack: OnMessageSendCallBack? = null
    private var otherTypingListener: OnPeerTypingListener? = null
    private var onWillSendMessageListener: OnWillSendMessageListener? = null
    private var recordTouchListener: OnChatRecordTouchListener? = null
    private var reactionMessageListener: OnReactionMessageListener? = null
    private var modifyMessageListener: OnModifyMessageListener? = null
    private var reportMessageListener: OnReportMessageListener? = null
    private var messagesAdapter: EaseMessagesAdapter? = null

    var conversationId: String? = null
    var chatType: EaseChatType? = null
    var historyMsgId: String? = null
    var isFromServer = false
    var isThread = false

    private var sendOriginalImage = false

    protected val attachmentController: EaseChatAttachmentController by lazy {
        EaseChatAttachmentController(mContext, binding?.layoutChat, conversationId, sendOriginalImage)
    }
    protected val mentionController: EaseChatMentionController by lazy {
        EaseChatMentionController(mContext,binding?.layoutChat )
    }

    private val launcherToCamera: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> attachmentController.onActivityResult(result, REQUEST_CODE_CAMERA) }
    private val launcherToAlbum: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> attachmentController.onActivityResult(result, REQUEST_CODE_LOCAL) }
    private val launcherToVideo: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> attachmentController.onActivityResult(result, REQUEST_CODE_SELECT_VIDEO) }
    private val launcherToFile: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> attachmentController.onActivityResult(result, REQUEST_CODE_SELECT_FILE) }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): EaseFragmentChatBinding? {
        return EaseFragmentChatBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        arguments?.let {
            conversationId = it.getString(EaseConstant.EXTRA_CONVERSATION_ID)
            chatType = EaseChatType.values()[it.getInt(
                EaseConstant.EXTRA_CHAT_TYPE,
                EaseChatType.SINGLE_CHAT.ordinal
            )]
            historyMsgId = it.getString(EaseConstant.HISTORY_MSG_ID)
            isFromServer = it.getBoolean(EaseConstant.EXTRA_IS_FROM_SERVER, false)
            isThread = it.getBoolean(Constant.KEY_THREAD_MESSAGE_FLAG, false)

            binding?.titleBar?.visibility = View.GONE
            val useHeader: Boolean = it.getBoolean(Constant.KEY_USE_TITLE, false)
            binding?.titleBar?.visibility = if (useHeader) View.VISIBLE else View.GONE
            if (useHeader) {
                binding?.chatHeaderDivider?.visibility = View.VISIBLE
                val title: String = it.getString(Constant.KEY_SET_TITLE, "")
                if (!TextUtils.isEmpty(title)) {
                    binding?.titleBar?.setTitle(title)
                }
                val subTitle: String = it.getString(Constant.KEY_SET_SUB_TITLE, "")
                if (!TextUtils.isEmpty(subTitle)) {
                    binding?.titleBar?.setSubtitle(subTitle)
                }
                val canBack: Boolean = it.getBoolean(Constant.KEY_ENABLE_BACK, false)
                binding?.titleBar?.setDisplayHomeAsUpEnabled(canBack
                    , it.getBoolean(Constant.KEY_USE_TITLE_REPLACE, false))
                binding?.titleBar?.setNavigationOnClickListener {
                    if (backPressListener != null) {
                        backPressListener?.onClick(it)
                        return@setNavigationOnClickListener
                    }
                    mContext.onBackPressed()
                }
                setDefaultHeader()
            }

            val timeColor: Int = it.getInt(Constant.KEY_MSG_TIME_COLOR, -1)
            if (timeColor != -1) {
                binding?.layoutChat?.chatMessageListLayout?.setTimeTextColor(timeColor)
            }
            val timeTextSize: Int = it.getInt(Constant.KEY_MSG_TIME_SIZE, -1)
            if (timeTextSize != -1) {
                binding?.layoutChat?.chatMessageListLayout?.setTimeTextSize(timeTextSize)
            }
            val leftBubbleBg: Int = it.getInt(Constant.KEY_MSG_LEFT_BUBBLE, -1)
            if (leftBubbleBg != -1) {
                binding?.layoutChat?.chatMessageListLayout?.
                setItemReceiverBackground(ContextCompat.getDrawable(mContext, leftBubbleBg))
            }
            val rightBubbleBg: Int = it.getInt(Constant.KEY_MSG_RIGHT_BUBBLE, -1)
            if (rightBubbleBg != -1) {
                binding?.layoutChat?.chatMessageListLayout?.
                setItemSenderBackground(ContextCompat.getDrawable(mContext, rightBubbleBg))
            }
            val showNickname: Boolean = it.getBoolean(Constant.KEY_SHOW_NICKNAME, false)
            binding?.layoutChat?.chatMessageListLayout?.showNickname(showNickname)
            val messageListShowType: String =
                it.getString(Constant.KEY_MESSAGE_LIST_SHOW_STYLE, "")
            if (!TextUtils.isEmpty(messageListShowType)) {
                EaseChatMessageListLayout.ShowType.valueOf(messageListShowType)?.let { type ->
                    binding?.layoutChat?.chatMessageListLayout?.setItemShowType(type)
                }
            }
            val hideReceiveAvatar: Boolean =
                it.getBoolean(Constant.KEY_HIDE_RECEIVE_AVATAR, false)
            binding?.layoutChat?.chatMessageListLayout?.hideChatReceiveAvatar(hideReceiveAvatar)
            val hideSendAvatar: Boolean = it.getBoolean(Constant.KEY_HIDE_SEND_AVATAR, false)
            binding?.layoutChat?.chatMessageListLayout?.hideChatSendAvatar(hideSendAvatar)
            val turnOnTypingMonitor: Boolean =
                it.getBoolean(Constant.KEY_TURN_ON_TYPING_MONITOR, false)
            binding?.layoutChat?.turnOnTypingMonitor(turnOnTypingMonitor)
            val chatBg: Int = it.getInt(Constant.KEY_CHAT_BACKGROUND, -1)
            if (chatBg != -1) {
                binding?.layoutChat?.chatMessageListLayout?.setBackgroundResource(chatBg)
            }
            val chatMenuStyle: String = it.getString(Constant.KEY_CHAT_MENU_STYLE, "")
            if (!TextUtils.isEmpty(chatMenuStyle)) {
                EaseInputMenuStyle.valueOf(chatMenuStyle)?.let { style ->
                    binding?.layoutChat?.chatInputMenu?.chatPrimaryMenu?.setMenuShowType(style)
                }
            }
            val inputBg: Int = it.getInt(Constant.KEY_CHAT_MENU_INPUT_BG, -1)
            if (inputBg != -1) {
                binding?.layoutChat?.chatInputMenu?.chatPrimaryMenu?.setMenuBackground(ContextCompat.getDrawable(mContext, inputBg))
            }
            val inputHint: String = it.getString(Constant.KEY_CHAT_MENU_INPUT_HINT, "")
            if (!TextUtils.isEmpty(inputHint)) {
                binding?.layoutChat?.chatInputMenu?.chatPrimaryMenu?.editText?.hint = inputHint
            }
            sendOriginalImage = it.getBoolean(Constant.KEY_SEND_ORIGINAL_IMAGE_MESSAGE, false)
            val emptyLayout: Int = it.getInt(Constant.KEY_EMPTY_LAYOUT, -1)
            if (emptyLayout != -1) {
                binding?.layoutChat?.chatMessageListLayout?.getMessagesAdapter()?.setEmptyView(emptyLayout)
            }
        }
        messagesAdapter?.let {
            binding?.layoutChat?.chatMessageListLayout?.setMessagesAdapter(it)
        }
        setCustomExtendView()
    }

    open fun setDefaultHeader() {
        EaseIM.getConfig()?.avatarConfig?.setAvatarStyle(binding?.titleBar?.getLogoView())
        chatType?.let { type ->
            EaseIM.getConversationInfoProvider()?.getSyncProfile(conversationId, type.getConversationType())?.let { profile ->
                binding?.run {
                    if (titleBar.getTitle().isNullOrEmpty()) {
                        titleBar.setTitle(profile.name)
                    }
                    val defaultRes = when(type) {
                        EaseChatType.GROUP_CHAT -> R.drawable.ease_default_group_avatar
                        EaseChatType.CHATROOM -> R.drawable.ease_default_chatroom_avatar
                        else -> R.drawable.ease_default_avatar
                    }
                    titleBar.setLogo(profile.avatar, defaultRes, resources.getDimensionPixelSize(R.dimen.ease_title_bar_icon_size))
                }
            } ?: kotlin.run {
                if (type == EaseChatType.SINGLE_CHAT) {
                    EaseIM.getUserProvider()?.getSyncUser(conversationId)?.let { user ->
                        binding?.run {
                            if (titleBar.getTitle().isNullOrEmpty()) {
                                titleBar.setTitle(user.name)
                            }
                            titleBar.setLogo(
                                user.avatar,
                                R.drawable.ease_default_avatar,
                                resources.getDimensionPixelSize(R.dimen.ease_title_bar_icon_size)
                            )
                        }
                    } ?: kotlin.run {
                        setDefaultInfo(type)
                    }
                } else {
                    setDefaultInfo(type)
                }
            }
        }

        binding?.titleBar?.getLogoView()?.setOnClickListener {
            when(chatType) {
                EaseChatType.GROUP_CHAT -> startActivity(EaseGroupDetailActivity.createIntent(mContext, conversationId!!))
                EaseChatType.SINGLE_CHAT -> {
                    startActivity(EaseContactDetailsActivity.createIntent(mContext
                        , EaseIM.getCache().getUser(conversationId!!)?.toUser() ?: EaseUser(conversationId!!)))
                }
                else -> {}
            }
        }
        binding?.titleBar?.setLogoClickListener{
            defaultSkipLogic()
        }
        binding?.titleBar?.setTitleClickListener{
            defaultSkipLogic()
        }
    }

    private fun defaultSkipLogic() {
        if (chatType == EaseChatType.SINGLE_CHAT) {
            startActivity(EaseContactDetailsActivity.createIntent(mContext
                , EaseIM.getCache().getUser(conversationId!!)?.toUser() ?: EaseUser(conversationId!!)))
        } else if (chatType == EaseChatType.GROUP_CHAT){
            startActivity(EaseGroupDetailActivity.createIntent(mContext, conversationId!!))
        }
    }

    private fun setDefaultInfo(chatType: EaseChatType) {
        if (binding?.titleBar?.getTitle().isNullOrEmpty()) {
            binding?.titleBar?.setTitle(when(chatType) {
                EaseChatType.GROUP_CHAT -> ChatClient.getInstance().groupManager().getGroup(conversationId)?.groupName ?: conversationId
                EaseChatType.CHATROOM -> ChatClient.getInstance().chatroomManager().getChatRoom(conversationId)?.name ?: conversationId
                else -> conversationId
            })
        }
        val defaultRes = when(chatType) {
            EaseChatType.GROUP_CHAT -> R.drawable.ease_default_group_avatar
            EaseChatType.CHATROOM -> R.drawable.ease_default_chatroom_avatar
            else -> R.drawable.ease_default_avatar
        }
        binding?.titleBar?.setLogo(defaultRes)
        binding?.titleBar?.setLogoSize(resources.getDimensionPixelSize(R.dimen.ease_title_bar_icon_size))
    }

    private fun setCustomExtendView() {
        val dialog = EaseChatExtendMenuDialog(mContext)
        dialog.init()
        binding?.layoutChat?.chatInputMenu?.setCustomExtendMenu(dialog)
    }

    override fun initListener() {
        super.initListener()
        binding?.layoutChat?.let {
            it.setOnChatLayoutListener(this)
            it.setOnMenuChangeListener(this)
            it.setOnWillSendMessageListener(this)
            it.setOnEditMessageListener(this)
            it.setOnReportMessageListener(this)
            it.setOnChatRecordTouchListener(recordTouchListener)
            it.setOnChatFinishListener(this)
            EaseIM.addMultiDeviceListener(this)
        }
    }

    override fun initData() {
        super.initData()
        initChatLayout()
        loadData()
        initEventBus()
    }

    open fun initEventBus() {
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.REMOVE.name).register(viewLifecycleOwner) {
            if (it.isConversationChange) {
                loadData()
            }
        }
        EaseFlowBus.withStick<EaseEvent>(EaseEvent.EVENT.REMOVE.name).register(viewLifecycleOwner) {
            if (it.isConversationChange) {
                loadData()
            }
        }
        EaseFlowBus.withStick<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(viewLifecycleOwner) {
            if (it.isMessageChange && !it.message.isNullOrEmpty()) {
                binding?.layoutChat?.chatMessageListLayout?.refreshMessage(it.message)
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.DESTROY.name).register(viewLifecycleOwner) {
            if (it.isGroupChange && it.message == conversationId) {
                finishCurrentActivity(EaseChatFinishReason.onGroupDestroyed)
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.LEAVE.name).register(viewLifecycleOwner) {
            if (it.isGroupChange && it.message == conversationId) {
                finishCurrentActivity(EaseChatFinishReason.onGroupLeft)
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.REMOVE.name).register(viewLifecycleOwner) {
            if (it.isContactChange && it.message == conversationId) {
                finishCurrentActivity(EaseChatFinishReason.onContactRemoved)
            }
        }
    }

    open fun initChatLayout() {
        binding?.layoutChat?.let {
            if (!TextUtils.isEmpty(historyMsgId)) {
                it.init(
                    conversationId,
                    chatType,
                    EaseLoadDataType.HISTORY
                )
            } else {
                if (isThread) {
                    it.init(
                        conversationId,
                        chatType,
                        EaseLoadDataType.HISTORY
                    )
                } else {
                    if (isFromServer) {
                        it.init(
                            conversationId,
                            chatType,
                            EaseLoadDataType.ROAM
                        )
                    } else {
                        it.init(
                            conversationId,
                            chatType
                        )
                    }
                }
            }
        }
    }

    open fun loadData() {
        if (!TextUtils.isEmpty(historyMsgId)) {
            binding?.layoutChat?.loadData(historyMsgId)
        } else {
            binding?.layoutChat?.loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EaseIM.removeMultiDeviceListener(this)
    }

    fun selectPicFromLocal() {
        attachmentController.selectPicFromLocal(launcherToAlbum)
    }

    fun selectVideoFromLocal() {
        attachmentController.selectVideoFromLocal(launcherToVideo)
    }

    fun selectFileFromLocal() {
        attachmentController.selectFileFromLocal(launcherToFile)
    }

    open fun finishCurrentActivity(reason: EaseChatFinishReason) {
        when(reason) {
            EaseChatFinishReason.onGroupLeft,
            EaseChatFinishReason.onGroupDestroyed,
            EaseChatFinishReason.onContactRemoved -> {
                EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name)
                    .post(lifecycleScope, EaseEvent(EaseEvent.EVENT.UPDATE.name, EaseEvent.TYPE.CONVERSATION, conversationId))
                mContext.finish()
            }
            else -> {}
        }
    }

    override fun onBubbleClick(message: ChatMessage?): Boolean {
        return chatItemClickListener?.onBubbleClick(message) ?: false
    }

    override fun onBubbleLongClick(v: View?, message: ChatMessage?): Boolean {
        return chatItemClickListener?.onBubbleLongClick(v, message) ?: false
    }

    override fun onResendClick(message: ChatMessage?): Boolean {
        return chatItemClickListener?.onResendClick(message) ?: false
    }

    override fun onUserAvatarClick(userId: String?) {
        chatItemClickListener?.onUserAvatarClick(userId)
        ?: kotlin.run {
            if (!userId.isNullOrEmpty() && userId != ChatClient.getInstance().currentUser) {
                val user = EaseIM.getUserProvider()?.getSyncUser(userId)?.toUser() ?: EaseUser(userId)
                startActivity(EaseContactDetailsActivity.createIntent(mContext, user))
            }
        }
    }

    override fun onUserAvatarLongClick(userId: String?) {
        chatItemClickListener?.onUserAvatarLongClick(userId)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val currentConversation = binding?.layoutChat?.chatMessageListLayout?.currentConversation
        currentConversation?.let { cv->
            binding?.layoutChat?.chatMessageListLayout?.isGroupChat(cv)?.let { isGroupChat->
                if (isGroupChat && s != null){
                    if (count == 1 && "@" == s[start].toString()) {
                        conversationId?.let { it1 -> mentionController.showMentionDialog(it1) }
                    }
                }
            }
        }
        chatInputChangeListener?.onTextChanged(s, start, before, count)
    }

    override fun afterTextChanged(s: Editable?) {
        s?.let { mentionController.setPickAtContentStyle(s) }
        chatInputChangeListener?.afterTextChanged(s)
    }

    override fun editTextOnKeyListener(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        return mentionController.removePickAt(v,keyCode,event)
    }

    override fun onChatFinish(reason: EaseChatFinishReason, id: String?) {
        finishCurrentActivity(reason)
    }

    override fun onSuccess(message: ChatMessage?) {
        super.onSuccess(message)
        messageSendCallBack?.onSuccess(message)
    }

    override fun onError(code: Int, errorMsg: String?) {
        messageSendCallBack?.onError(code, errorMsg)
    }

    override fun onChatExtendMenuItemClick(view: View?, itemId: Int): Boolean {
        if (extendMenuItemClickListener != null && extendMenuItemClickListener?.onChatExtendMenuItemClick(view, itemId) == true) {
            return true
        }
        when(itemId) {
            R.id.extend_item_take_picture -> {
                attachmentController.selectPicFromCamera(launcherToCamera)
                return true
            }
            R.id.extend_item_picture -> {
                attachmentController.selectPicFromLocal(launcherToAlbum)
                return true
            }
            R.id.extend_item_video -> {
                attachmentController.selectVideoFromLocal(launcherToVideo)
                return true
            }
            R.id.extend_item_file -> {
                attachmentController.selectFileFromLocal(launcherToFile)
                return true
            }
            R.id.extend_item_contact_card -> {
                attachmentController.selectContact(childFragmentManager, chatType!!)
                return true
            }
        }
        return false
    }

    override fun onPreMenu(helper: EaseChatMenuHelper?, message: ChatMessage?) {

    }

    override fun onMenuItemClick(item: EaseMenuItem?, message: ChatMessage?): Boolean {
        return false
    }

    override fun onWillSendMessage(message: ChatMessage?) {
        this.onWillSendMessageListener?.onWillSendMessage(message)
    }

    override fun onModifyMessageSuccess(messageModified: ChatMessage?) {
        modifyMessageListener?.onModifyMessageSuccess(messageModified)
    }

    override fun onModifyMessageFailure(messageId: String?, code: Int, error: String?) {
        modifyMessageListener?.onModifyMessageFailure(messageId, code, error)
    }

    override fun onReportMessageSuccess(msgId:String) {
        reportMessageListener?.onReportMessageSuccess(msgId)
        ?: kotlin.run {
            mContext.showToast(R.string.ease_report_success)
        }
    }

    override fun onReportMessageFailure(msgId: String?, code: Int, error: String?) {
        reportMessageListener?.onReportMessageFailure(msgId,code, error)
    }

    override fun onPeerTyping(action: String?) {
        otherTypingListener?.onPeerTyping(action)
    }

    override fun onContactEvent(event: Int, target: String?, ext: String?) {

    }

    override fun onGroupEvent(event: Int, target: String?, usernames: MutableList<String>?) {
        if (event == GROUP_DESTROY || event == GROUP_LEAVE) {
            if (TextUtils.equals(target, conversationId)) {
                mContext.finish()
            }
        }
    }

    private fun setHeaderBackPressListener(listener: View.OnClickListener?) {
        this.backPressListener = listener
    }

    private fun setOnChatExtendMenuItemClickListener(listener: OnChatExtendMenuItemClickListener?) {
        this.extendMenuItemClickListener = listener
    }

    private fun setOnChatInputChangeListener(listener: OnChatInputChangeListener?) {
        this.chatInputChangeListener = listener
    }

    private fun setOnMessageItemClickListener(listener: OnMessageItemClickListener?) {
        this.chatItemClickListener = listener
    }

    private fun setOnMessageSendCallBack(callBack: OnMessageSendCallBack?) {
        this.messageSendCallBack = callBack
    }

    private fun setOnPeerTypingListener(listener: OnPeerTypingListener?) {
        this.otherTypingListener = listener
    }

    private fun setOnWillSendMessageListener(onWillSendMessageListener: OnWillSendMessageListener?) {
        this.onWillSendMessageListener = onWillSendMessageListener
    }

    private fun setOnChatRecordTouchListener(recordTouchListener: OnChatRecordTouchListener?) {
        this.recordTouchListener = recordTouchListener
    }

    private fun setOnReactionMessageListener(reactionMessageListener: OnReactionMessageListener?) {
        this.reactionMessageListener = reactionMessageListener
    }

    private fun setOnModifyMessageListener(listener: OnModifyMessageListener?) {
        this.modifyMessageListener = listener
    }

    private fun setOnReportMessageListener(listener: OnReportMessageListener?){
        this.reportMessageListener = listener
    }

    private fun setCustomAdapter(adapter: EaseMessagesAdapter?) {
        this.messagesAdapter = adapter
    }

    class Builder(
        private val conversationId: String?,
        private val chatType: EaseChatType = EaseChatType.SINGLE_CHAT,
        private val historyMsgId: String? = null
    ) {
        protected val bundle: Bundle = Bundle()
        private var backPressListener: View.OnClickListener? = null
        private var adapter: EaseMessagesAdapter? = null
        private var extendMenuItemClickListener: OnChatExtendMenuItemClickListener? = null
        private var chatInputChangeListener: OnChatInputChangeListener? = null
        private var messageItemClickListener: OnMessageItemClickListener? = null
        private var messageSendCallBack: OnMessageSendCallBack? = null
        private var peerTypingListener: OnPeerTypingListener? = null
        private var willSendMessageListener: OnWillSendMessageListener? = null
        private var recordTouchListener: OnChatRecordTouchListener? = null
        private var reactionMessageListener: OnReactionMessageListener? = null
        protected var customFragment: EaseChatFragment? = null
        private var modifyMessageListener: OnModifyMessageListener? = null
        private var reportMessageListener: OnReportMessageListener? = null

        init {
            bundle.putString(EaseConstant.EXTRA_CONVERSATION_ID, conversationId)
            bundle.putInt(EaseConstant.EXTRA_CHAT_TYPE, chatType.ordinal)
            bundle.putString(EaseConstant.HISTORY_MSG_ID, historyMsgId)
        }

        /**
         * Set history message id.
         *
         * @param historyMessageId
         * @return
         */
        fun setHistoryMessageId(historyMessageId: String?): Builder {
            bundle.putString(EaseConstant.HISTORY_MSG_ID, historyMessageId)
            return this
        }

        /**
         * Whether to use default titleBar which is [EaseTitleBar]
         *
         * @param useTitle
         * @return
         */
        fun useTitleBar(useTitle: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_USE_TITLE, useTitle)
            return this
        }

        /**
         * Whether to use default titleBar to replace actionBar when activity is a AppCompatActivity.
         * If set true, will call [androidx.appcompat.app.AppCompatActivity.setSupportActionBar].
         * @param replace
         * @return
         */
        fun useTitleBarToReplaceActionBar(replace: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_USE_TITLE_REPLACE, replace)
            return this
        }

        /**
         * Set titleBar's title
         *
         * @param title
         * @return
         */
        fun setTitleBarTitle(title: String?): Builder {
            bundle.putString(Constant.KEY_SET_TITLE, title)
            return this
        }

        /**
         * Set titleBar's sub title
         *
         * @param subTitle
         * @return
         */
        fun setTitleBarSubTitle(subTitle: String?): Builder {
            bundle.putString(Constant.KEY_SET_SUB_TITLE, subTitle)
            return this
        }

        /**
         * Whether show back icon in titleBar
         *
         * @param canBack
         * @return
         */
        fun enableTitleBarPressBack(canBack: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_ENABLE_BACK, canBack)
            return this
        }

        /**
         * If you have set [Builder.enableTitleBarPressBack], you can set the listener
         *
         * @param listener
         * @return
         */
        fun setTitleBarBackPressListener(listener: View.OnClickListener?): Builder {
            backPressListener = listener
            return this
        }

        /**
         * Set Whether to get history message from server or local database
         *
         * @param isFromServer
         * @return
         */
        fun getHistoryMessageFromServerOrLocal(isFromServer: Boolean): Builder {
            bundle.putBoolean(EaseConstant.EXTRA_IS_FROM_SERVER, isFromServer)
            return this
        }

        /**
         * Set chat extension menu item click listener
         *
         * @param listener
         * @return
         */
        fun setOnChatExtendMenuItemClickListener(listener: OnChatExtendMenuItemClickListener?): Builder {
            extendMenuItemClickListener = listener
            return this
        }

        /**
         * Set chat menu's text change listener
         *
         * @param listener
         * @return
         */
        fun setOnChatInputChangeListener(listener: OnChatInputChangeListener?): Builder {
            chatInputChangeListener = listener
            return this
        }

        /**
         * Set message item click listener, include bubble click, bubble long click, avatar click
         * and avatar long click
         *
         * @param listener
         * @return
         */
        fun setOnMessageItemClickListener(listener: OnMessageItemClickListener?): Builder {
            messageItemClickListener = listener
            return this
        }

        /**
         * Set message's callback after which is sent
         *
         * @param callBack
         * @return
         */
        fun setOnMessageSendCallBack(callBack: OnMessageSendCallBack?): Builder {
            messageSendCallBack = callBack
            return this
        }

        /**
         * Turn on other peer's typing monitor, only for single chat
         *
         * @param turnOn
         * @return
         */
        internal fun turnOnTypingMonitor(turnOn: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_TURN_ON_TYPING_MONITOR, turnOn)
            return this
        }

        /**
         * Set peer's typing listener, only for single chat. You need call [Builder.turnOnTypingMonitor] first.
         *
         * @param listener
         * @return
         */
        internal fun setOnPeerTypingListener(listener: OnPeerTypingListener?): Builder {
            peerTypingListener = listener
            return this
        }

        /**
         * Set the event you can add message's attrs before send message
         *
         * @param willSendMessageListener
         * @return
         */
        fun setOnWillSendMessageListener(willSendMessageListener: OnWillSendMessageListener?): Builder {
            this.willSendMessageListener = willSendMessageListener
            return this
        }

        /**
         * Set touch event listener during recording
         *
         * @param recordTouchListener
         * @return
         */
        fun setOnChatRecordTouchListener(recordTouchListener: OnChatRecordTouchListener?): Builder {
            this.recordTouchListener = recordTouchListener
            return this
        }

        /**
         * Set reaction listener
         *
         * @param reactionMessageListener
         * @return
         */
        private fun setOnReactionMessageListener(reactionMessageListener: OnReactionMessageListener?): Builder {
            this.reactionMessageListener = reactionMessageListener
            return this
        }

        /**
         * Set the text color of message item time
         *
         * @param color
         * @return
         */
        fun setMsgTimeTextColor(@ColorInt color: Int): Builder {
            bundle.putInt(Constant.KEY_MSG_TIME_COLOR, color)
            return this
        }

        /**
         * Set the text size of message item time, unit is px
         *
         * @param size
         * @return
         */
        fun setMsgTimeTextSize(size: Int): Builder {
            bundle.putInt(Constant.KEY_MSG_TIME_SIZE, size)
            return this
        }

        /**
         * Set the bubble background of the received message
         *
         * @param bgDrawable
         * @return
         */
        fun setReceivedMsgBubbleBackground(@DrawableRes bgDrawable: Int): Builder {
            bundle.putInt(Constant.KEY_MSG_LEFT_BUBBLE, bgDrawable)
            return this
        }

        /**
         * Set the bubble background of the sent message
         *
         * @param bgDrawable
         * @return
         */
        fun setSentBubbleBackground(@DrawableRes bgDrawable: Int): Builder {
            bundle.putInt(Constant.KEY_MSG_RIGHT_BUBBLE, bgDrawable)
            return this
        }

        /**
         * Whether to show nickname in message item
         *
         * @param showNickname
         * @return
         */
        fun showNickname(showNickname: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_SHOW_NICKNAME, showNickname)
            return this
        }

        /**
         * Set message list show style, including normal and all_start style
         *
         * @param showType
         * @return
         */
        internal fun setMessageListShowStyle(showType: EaseChatMessageListLayout.ShowType): Builder {
            bundle.putString(Constant.KEY_MESSAGE_LIST_SHOW_STYLE, showType.name)
            return this
        }

        /**
         * Set the message modification listener.
         * @param listener
         * @return
         */
        fun setOnModifyMessageListener(listener: OnModifyMessageListener?): Builder {
            modifyMessageListener = listener
            return this
        }

        /**
         * Set the message report listener.
         * @param listener
         * @return
         */
        fun setOnReportMessageListener(listener: OnReportMessageListener?): Builder {
            reportMessageListener = listener
            return this
        }

        /**
         * Whether to hide receiver's avatar
         *
         * @param hide
         * @return
         */
        fun hideReceiverAvatar(hide: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_HIDE_RECEIVE_AVATAR, hide)
            return this
        }

        /**
         * Whether to hide sender's avatar
         *
         * @param hide
         * @return
         */
        fun hideSenderAvatar(hide: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_HIDE_SEND_AVATAR, hide)
            return this
        }

        /**
         * Set the background of the chat list region
         *
         * @param bgDrawable
         * @return
         */
        fun setChatBackground(@DrawableRes bgDrawable: Int): Builder {
            bundle.putInt(Constant.KEY_CHAT_BACKGROUND, bgDrawable)
            return this
        }

        /**
         * Set chat input menu style, including voice input, text input,
         * emoji input and extended function input
         *
         * @param style
         * @return
         */
        internal fun setChatInputMenuStyle(style: EaseInputMenuStyle): Builder {
            bundle.putString(Constant.KEY_CHAT_MENU_STYLE, style.name)
            return this
        }

        /**
         * Set chat input menu background
         *
         * @param bgDrawable
         * @return
         */
        fun setChatInputMenuBackground(@DrawableRes bgDrawable: Int): Builder {
            bundle.putInt(Constant.KEY_CHAT_MENU_INPUT_BG, bgDrawable)
            return this
        }

        /**
         * Set chat input menu's hint text
         *
         * @param inputHint
         * @return
         */
        fun setChatInputMenuHint(inputHint: String?): Builder {
            bundle.putString(Constant.KEY_CHAT_MENU_INPUT_HINT, inputHint)
            return this
        }

        /**
         * Set whether to use original file to send image message
         *
         * @param sendOriginalImage
         * @return
         */
        fun sendMessageByOriginalImage(sendOriginalImage: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_SEND_ORIGINAL_IMAGE_MESSAGE, sendOriginalImage)
            return this
        }

        /**
         * Marks whether the chat is a chat thread.
         *
         * @param isThread
         * @return
         */
        internal fun setThreadMessage(isThread: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_THREAD_MESSAGE_FLAG, isThread)
            return this
        }

        /**
         * Set chat list's empty layout if you want replace the default
         *
         * @param emptyLayout
         * @return
         */
        fun setEmptyLayout(@LayoutRes emptyLayout: Int): Builder {
            bundle.putInt(Constant.KEY_EMPTY_LAYOUT, emptyLayout)
            return this
        }

        /**
         * Set custom fragment which should extends EaseMessageFragment
         *
         * @param fragment
         * @param <T>
         * @return
        </T> */
        fun <T : EaseChatFragment?> setCustomFragment(fragment: T): Builder {
            customFragment = fragment
            return this
        }

        /**
         * Set custom adapter which should extends EaseMessageAdapter
         *
         * @param adapter
         * @return
         */
        fun setCustomAdapter(adapter: EaseMessagesAdapter?): Builder {
            this.adapter = adapter
            return this
        }

        fun build(): EaseChatFragment? {
            val fragment = if (customFragment != null) customFragment else EaseChatFragment()
            fragment?.let {
                it.arguments = bundle
                it.setHeaderBackPressListener(backPressListener)
                it.setOnChatExtendMenuItemClickListener(extendMenuItemClickListener)
                it.setOnChatInputChangeListener(chatInputChangeListener)
                it.setOnMessageItemClickListener(messageItemClickListener)
                it.setOnMessageSendCallBack(messageSendCallBack)
                it.setOnPeerTypingListener(peerTypingListener)
                it.setOnWillSendMessageListener(willSendMessageListener)
                it.setOnChatRecordTouchListener(recordTouchListener)
                it.setCustomAdapter(adapter)
                it.setOnReactionMessageListener(reactionMessageListener)
                it.setOnModifyMessageListener(modifyMessageListener)
                it.setOnReportMessageListener(reportMessageListener)
            }
            return fragment
        }
    }

    private object Constant {
        const val KEY_USE_TITLE = "key_use_title"
        const val KEY_USE_TITLE_REPLACE = "key_use_replace_action_bar"
        const val KEY_SET_TITLE = "key_set_title"
        const val KEY_SET_SUB_TITLE = "key_set_sub_title"
        const val KEY_EMPTY_LAYOUT = "key_empty_layout"
        const val KEY_ENABLE_BACK = "key_enable_back"
        const val KEY_MSG_TIME_COLOR = "key_msg_time_color"
        const val KEY_MSG_TIME_SIZE = "key_msg_time_size"
        const val KEY_MSG_LEFT_BUBBLE = "key_msg_left_bubble"
        const val KEY_MSG_RIGHT_BUBBLE = "key_msg_right_bubble"
        const val KEY_SHOW_NICKNAME = "key_show_nickname"
        const val KEY_MESSAGE_LIST_SHOW_STYLE = "key_message_list_show_type"
        const val KEY_HIDE_RECEIVE_AVATAR = "key_hide_left_avatar"
        const val KEY_HIDE_SEND_AVATAR = "key_hide_right_avatar"
        const val KEY_CHAT_BACKGROUND = "key_chat_background"
        const val KEY_CHAT_MENU_STYLE = "key_chat_menu_style"
        const val KEY_CHAT_MENU_INPUT_BG = "key_chat_menu_input_bg"
        const val KEY_CHAT_MENU_INPUT_HINT = "key_chat_menu_input_hint"
        const val KEY_TURN_ON_TYPING_MONITOR = "key_turn_on_typing_monitor"
        const val KEY_SEND_ORIGINAL_IMAGE_MESSAGE = "key_send_original_image_message"
        const val KEY_THREAD_MESSAGE_FLAG = "key_thread_message_flag"
    }

    companion object {
        const val TAG = "EaseChatFragment"
        const val REQUEST_CODE_CAMERA = 2
        const val REQUEST_CODE_LOCAL = 3
        const val REQUEST_CODE_DING_MSG = 4
        const val REQUEST_CODE_SELECT_VIDEO = 11
        const val REQUEST_CODE_SELECT_FILE = 12
    }

}