package com.hyphenate.easeui.feature.chat.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatConversation
import com.hyphenate.easeui.common.ChatConversationType
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.Chatroom
import com.hyphenate.easeui.common.RefreshHeader
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.lifecycleScope
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.impl.OnItemClickListenerImpl
import com.hyphenate.easeui.databinding.UikitChatMessageListBinding
import com.hyphenate.easeui.feature.chat.enums.ChatUIKitType
import com.hyphenate.easeui.feature.chat.enums.ChatUIKitLoadDataType
import com.hyphenate.easeui.feature.chat.adapter.ChatUIKitMessagesAdapter
import com.hyphenate.easeui.feature.chat.config.ChatUIKitMessageItemConfig
import com.hyphenate.easeui.feature.chat.controllers.ChatUIKitMessageListScrollAndDataController
import com.hyphenate.easeui.feature.chat.enums.getConversationType
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageItemStyle
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageListLayout
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageListResultView
import com.hyphenate.easeui.feature.chat.interfaces.OnChatErrorListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListItemClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListTouchListener
import com.hyphenate.easeui.feature.chat.enums.isShouldStackFromEnd
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageAckSendCallback
import com.hyphenate.easeui.feature.chat.reaction.interfaces.OnChatUIKitReactionErrorListener
import com.hyphenate.easeui.feature.chat.reply.interfaces.OnMessageReplyViewClickListener
import com.hyphenate.easeui.feature.thread.interfaces.OnMessageChatThreadClickListener
import com.hyphenate.easeui.model.ChatUIKitEvent
import com.hyphenate.easeui.viewmodel.messages.ChatUIKitMessageListViewModel
import com.hyphenate.easeui.viewmodel.messages.IChatMessageListRequest
import com.hyphenate.easeui.widget.ChatUIKitImageView.ShapeType
import com.hyphenate.easeui.widget.RefreshLayout
import kotlinx.coroutines.launch

class ChatUIKitMessageListLayout @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), IChatMessageListLayout,
    IChatMessageItemStyle, IChatMessageListResultView {

    private val binding: UikitChatMessageListBinding by lazy {
        UikitChatMessageListBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * Concat adapter
     */
    private val concatAdapter: ConcatAdapter by lazy {
        val config = ConcatAdapter.Config.Builder()
            .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
            .build()
        ConcatAdapter(config)
    }

    private val listScrollController: ChatUIKitMessageListScrollAndDataController by lazy {
        ChatUIKitMessageListScrollAndDataController(binding.messageList, messagesAdapter!!, context)
    }

    /**
     * 加载数据的模式
     */
    private var loadDataType: ChatUIKitLoadDataType = ChatUIKitLoadDataType.LOCAL

    /**
     * The configuration to set the style of the message item.
     */
    private lateinit var itemConfig: ChatUIKitMessageItemConfig

    /**
     * The viewModel to request data.
     */
    private var viewModel: IChatMessageListRequest? = null

    /**
     * The conversation to handle messages.
     */
    private var conversation: ChatConversation? = null

    /**
     * The adapter to show messages.
     */
    private var messagesAdapter: ChatUIKitMessagesAdapter? = null

    /**
     * The item click listener.
     */
    private var itemMessageClickListener: OnMessageListItemClickListener? = null

    /**
     * The message list touch listener.
     */
    private var messageTouchListener: OnMessageListTouchListener? = null

    /**
     * The message reply view click listener.
     */
    private var messageReplyViewClickListener: OnMessageReplyViewClickListener? = null

    /**
     * The message thread view click listener.
     */
    private var messageThreadViewClickListener:OnMessageChatThreadClickListener? = null

    /**
     * The error listener in chat message list.
     */
    private var chatErrorListener: OnChatErrorListener? = null

    /**
     * The message ack send callback.
     */
    private var messageAckSendCallback: OnMessageAckSendCallback? = null

    /**
     * The label that whether load the latest messages.
     */
    private var isSearchLatestMessages: Boolean = false

    private var baseSearchMessageId: String? = null

    /**
     * The label whether the first time to load data.
     */
    private var isFirstLoadData: Boolean = true


    init {
        initAttrs(context, attrs)
        initViews()
        initListener()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        itemConfig = ChatUIKitMessageItemConfig(context, attrs)
    }

    private fun initViews() {
        if (viewModel == null) {
            viewModel = if (context is AppCompatActivity) {
                ViewModelProvider(context)[ChatUIKitMessageListViewModel::class.java]
            } else {
                ChatUIKitMessageListViewModel()
            }
        }
        viewModel?.attachView(this)

        binding.messageList.layoutManager = LinearLayoutManager(context)
        messagesAdapter = ChatUIKitMessagesAdapter(itemConfig)
        messagesAdapter?.setHasStableIds(true)
        concatAdapter.addAdapter(messagesAdapter!!)
        binding.messageList.adapter = concatAdapter

        if (binding.messagesRefresh.refreshHeader == null) {
            binding.messagesRefresh.setRefreshHeader(RefreshHeader(context))
        }

        // Set not enable to load more.
        binding.messagesRefresh.setEnableLoadMore(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        binding.messagesRefresh.setOnRefreshListener {
            // load more older data
            loadMorePreviousData()
        }
        binding.messagesRefresh.setOnLoadMoreListener {
            // load more newer data
            loadMoreNewerData()
        }

        binding.messageList.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                listScrollController.onScrollStateChanged()
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    messageTouchListener?.onFinishScroll()
                    if (!this@ChatUIKitMessageListLayout.binding.messageList.canScrollVertically(1)) {
                        messageTouchListener?.onReachBottom()
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isFirstLoadData = false
                    //if recyclerView not idle should hide keyboard
                    messageTouchListener?.onViewDragging()
                }
            }
        })

        setAdapterListener()

        if (context is AppCompatActivity) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    if (context.isFinishing) {
                        makeAllMessagesAsRead(true)
                        context.lifecycle.removeObserver(this)
                    }
                }
            })
        }

        val gestureDetector = GestureDetector(context,object: GestureDetector.OnGestureListener {
            override fun onDown(e: MotionEvent): Boolean {
                messageTouchListener?.onTouchItemOutside(null, -1)
                return false
            }

            override fun onShowPress(e: MotionEvent) {

            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true;
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {

            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return false
            }
        })

        binding.messageList.setOnTouchListener { v, event -> gestureDetector.onTouchEvent(event) }

        binding.messageList.addOnLayoutChangeListener(object: OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                if (isFirstLoadData && loadDataType != ChatUIKitLoadDataType.SEARCH) {
                    listScrollController.smoothScrollToBottom()
                } else {
                    binding.messageList.removeOnLayoutChangeListener(this)
                }
            }
        })
    }

    private fun setAdapterListener() {
        messagesAdapter?.run {
            setOnItemClickListener(OnItemClickListenerImpl{ view, position ->
                // Add touch listener
                messageTouchListener?.onTouchItemOutside(view, position)
            })
            setOnMessageListItemClickListener(itemMessageClickListener)
            setOnMessageReplyViewClickListener(messageReplyViewClickListener)
            setOnMessageThreadEventListener(messageThreadViewClickListener)
            setOnMessageReactionErrorListener(object : OnChatUIKitReactionErrorListener {

                override fun onError(messageId: String, errorCode: Int, errorMessage: String?) {
                    chatErrorListener?.onChatError(errorCode, errorMessage)
                }
            })
            setOnMessageAckSendCallback(object: OnMessageAckSendCallback {
                override fun onSendAckSuccess(message: ChatMessage?) {
                    super.onSendAckSuccess(message)
                    messageAckSendCallback?.onSendAckSuccess(message)
                }

                override fun onSendAckError(message: ChatMessage?, code: Int, errorMsg: String?) {
                    ChatLog.e(TAG, "onSendAckError: $code, $errorMsg")
                    messageAckSendCallback?.onSendAckError(message, code, errorMsg)
                }
            })
        }
    }

    @JvmOverloads
    fun init(conversationId: String?, chatType: ChatUIKitType
             , loadDataType: ChatUIKitLoadDataType = ChatUIKitLoadDataType.LOCAL) {
        this.loadDataType = loadDataType
        conversation = ChatClient.getInstance().chatManager().getConversation(conversationId
            , chatType.getConversationType(), true, loadDataType == ChatUIKitLoadDataType.THREAD)

        viewModel?.setupWithConversation(conversation)

        // If it is chat thread, no more data can be loaded when the layout is pulled down.
        if (loadDataType == ChatUIKitLoadDataType.THREAD) {
            binding.messagesRefresh.setEnableRefresh(false)
            isCanAutoScrollToBottom = false
        }

        if (loadDataType == ChatUIKitLoadDataType.THREAD || loadDataType == ChatUIKitLoadDataType.SEARCH) {
            binding.messagesRefresh.setEnableLoadMore(true)
        }

        // Set whether load data from top or from bottom.
        //setListStackFromEnd()
    }

    /**
     * Call this method to load data.
     * @param messageId When the loadDataType is history, the param needs to be set to search history message list.
     * @param pageSize If you want to change the amount of data pulled each time, you can set parameters.
     */
    fun loadData(messageId: String? = "", pageSize: Int = 10) {
        baseSearchMessageId = messageId
        viewModel?.pageSize = pageSize
        conversation?.run {
            // If is chatroom type, should join the chatroom first
            if (isChatroom(this)) {
                viewModel?.joinChatroom(conversationId())
            } else {
                loadMessages()
            }
        }
    }

    private fun loadMessages() {
        conversation?.run {
            if (!isSingleChat(this)) itemConfig.showNickname = true
            // Mark all message as read
            makeAllMessagesAsRead()
            when(loadDataType) {
                ChatUIKitLoadDataType.ROAM -> viewModel?.fetchRoamMessages()
                ChatUIKitLoadDataType.SEARCH -> viewModel?.loadLocalHistoryMessages(baseSearchMessageId, ChatSearchDirection.DOWN, true)
                ChatUIKitLoadDataType.THREAD -> viewModel?.fetchRoamMessages(direction = ChatSearchDirection.DOWN)
                else -> viewModel?.loadLocalMessages()
            }
        }
    }

    private fun makeAllMessagesAsRead(sendEvent: Boolean = false) {
        conversation?.run {
            markAllMessagesAsRead()
            if (sendEvent) {
                sendReadEvent(conversationId())
            }
        }
    }

    private fun sendReadEvent(conversationId: String) {
        // Send update event
        ChatUIKitFlowBus.withStick<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name)
            .post(context.mainScope()
                , ChatUIKitEvent(
                    ChatUIKitEvent.EVENT.UPDATE.name
                    , ChatUIKitEvent.TYPE.CONVERSATION, conversationId)
            )
    }

    private fun loadMorePreviousData() {
        if (loadDataType == ChatUIKitLoadDataType.THREAD) return
        val firstMsgId = messagesAdapter?.mData?.let {
            if (it.isNotEmpty()){
                it.first().msgId
            }else ""
        }
        when (loadDataType) {
            ChatUIKitLoadDataType.ROAM -> {
                viewModel?.fetchMoreRoamMessages(firstMsgId)
            }
            ChatUIKitLoadDataType.SEARCH -> {
                viewModel?.loadLocalHistoryMessages(firstMsgId, ChatSearchDirection.UP)
            }
            else -> {
                viewModel?.loadMoreLocalMessages(firstMsgId)
            }
        }
    }

    private fun loadMoreNewerData() {
        messagesAdapter?.mData?.let {
            if (it.isNotEmpty()){
                val lastMsgId = it.last().msgId
                when (loadDataType) {
                    ChatUIKitLoadDataType.SEARCH -> {
                        viewModel?.loadLocalHistoryMessages(lastMsgId, ChatSearchDirection.DOWN)
                    }
                    ChatUIKitLoadDataType.THREAD -> {
                        viewModel?.fetchMoreRoamMessages(lastMsgId, ChatSearchDirection.DOWN)
                    }
                    else -> {}
                }
            }else{
                finishRefresh()
            }
        }
    }

    private fun setListStackFromEnd() {
        binding.messageList.layoutManager?.let {
            if (it is LinearLayoutManager) {
                it.stackFromEnd = loadDataType.isShouldStackFromEnd()
            }
            if (it is ChatUIKitCustomLayoutManager) {
                it.setIsStackFromEnd(loadDataType.isShouldStackFromEnd())
            }
        }
    }

    override val currentConversation: ChatConversation?
        get() = this.conversation
    override var isCanAutoScrollToBottom: Boolean
        get() = listScrollController.isCanAutoScrollToBottom()
        set(value)  = listScrollController.setCanAutoScrollToBottom(value)

    private fun isChatroom(conv: ChatConversation): Boolean {
        return conv.type == ChatConversationType.ChatRoom && loadDataType != ChatUIKitLoadDataType.THREAD
    }

    fun isGroupChat(conv: ChatConversation): Boolean {
        return conv.type == ChatConversationType.GroupChat && loadDataType != ChatUIKitLoadDataType.THREAD
    }

    private fun isSingleChat(conv: ChatConversation): Boolean {
        return conv.type == ChatConversationType.Chat
    }

    private fun finishRefresh() {
        context.mainScope().launch {
            binding.messagesRefresh.finishRefresh()
            binding.messagesRefresh.finishLoadMore()
        }
    }

    private fun enableLoadMore(enable: Boolean) {
        context.mainScope().launch {
            binding.messagesRefresh.setEnableLoadMore(enable)
        }
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setAvatarDefaultSrc(src: Drawable?) {
        itemConfig.avatarSrc = src
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setAvatarShapeType(shapeType: ShapeType) {
        itemConfig.avatarConfig.avatarShape = shapeType
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun showNickname(showNickname: Boolean) {
        itemConfig.showNickname = showNickname
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemSenderBackground(bg: Int?) {
        itemConfig.senderBackground = bg
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemReceiverBackground(bg: Int?) {
        itemConfig.receiverBackground = bg
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemTextSize(textSize: Int) {
        itemConfig.textSize = textSize
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemTextColor(textColor: Int) {
        itemConfig.textColor = textColor
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeTextSize(textSize: Int) {
        itemConfig.timeTextSize = textSize
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeTextColor(textColor: Int) {
        itemConfig.timeTextColor = textColor
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeBackground(bgDrawable: Drawable?) {
        itemConfig.timeBackground = bgDrawable
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemShowType(type: ShowType) {
        itemConfig.showType = type
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun hideChatReceiveAvatar(hide: Boolean) {
        itemConfig.hideReceiverAvatar = hide
    }

    /**
     * After setting the parameter, should call the method [ChatUIKitMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun hideChatSendAvatar(hide: Boolean) {
        itemConfig.hideSenderAvatar = hide
    }

    override val refreshLayout: RefreshLayout?
        get() = binding.messagesRefresh
    override val messageListLayout: RecyclerView?
        get() = binding.messageList

    override fun setViewModel(viewModel: IChatMessageListRequest?) {
        this.viewModel = viewModel
        this.viewModel?.let {
            it.attachView(this)
            it.setupWithConversation(conversation)
        }
    }

    override fun setMessagesAdapter(adapter: ChatUIKitMessagesAdapter?) {
        adapter?.let {
            if (this.messagesAdapter != null && concatAdapter.adapters
                    .contains(this.messagesAdapter)
            ) {
                val index: Int = concatAdapter.adapters.indexOf(this.messagesAdapter)
                concatAdapter.removeAdapter(messagesAdapter!!)
                it.setHasStableIds(true)
                concatAdapter.addAdapter(index, it)
            } else {
                it.setHasStableIds(true)
                concatAdapter.addAdapter(it)
            }
            this.messagesAdapter = it
            setAdapterListener()
            notifyDataSetChanged()
        }
    }

    override fun getMessagesAdapter(): ChatUIKitMessagesAdapter? {
        return messagesAdapter
    }

    override fun setOnMessageListTouchListener(listener: OnMessageListTouchListener?) {
        this.messageTouchListener = listener
    }

    override fun setOnMessageListItemClickListener(listener: OnMessageListItemClickListener?) {
        this.itemMessageClickListener = listener
        messagesAdapter?.let {
            it.setOnMessageListItemClickListener(listener)
            it.notifyDataSetChanged()
        }
    }

    override fun setOnMessageReplyViewClickListener(listener: OnMessageReplyViewClickListener?) {
        this.messageReplyViewClickListener = listener
        messagesAdapter?.let {
            it.setOnMessageReplyViewClickListener(listener)
            it.notifyDataSetChanged()
        }
    }

    override fun setOnMessageThreadViewClickListener(listener: OnMessageChatThreadClickListener?) {
        this.messageThreadViewClickListener = listener
        messagesAdapter?.let {
            it.setOnMessageThreadEventListener(listener)
            it.notifyDataSetChanged()
        }
    }

    override fun setOnMessageAckSendCallback(callback: OnMessageAckSendCallback?) {
        this.messageAckSendCallback = callback
    }

    override fun setOnChatErrorListener(listener: OnChatErrorListener?) {
        this.chatErrorListener = listener
    }

    override fun useDefaultRefresh(useDefaultRefresh: Boolean) {
        if (!useDefaultRefresh) {
            binding.messagesRefresh.setEnableLoadMore(false)
            binding.messagesRefresh.setEnableRefresh(false)
        }
    }

    override fun refreshMessages() {
        if (loadDataType != ChatUIKitLoadDataType.SEARCH || (loadDataType == ChatUIKitLoadDataType.SEARCH && isSearchLatestMessages)) {
            viewModel?.getAllCacheMessages()
        }
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        messagesAdapter?.setItemConfig(itemConfig)
        lifecycleScope.launch {
            messagesAdapter?.notifyDataSetChanged()
        }
    }

    override fun refreshToLatest() {
        if (loadDataType != ChatUIKitLoadDataType.SEARCH || (loadDataType == ChatUIKitLoadDataType.SEARCH && isSearchLatestMessages)) {
            viewModel?.getAllCacheMessages()
        }
        listScrollController.scrollToBottom(true)
    }

    override fun refreshMessage(messageId: String?) {
        refreshMessage(messageId?.let { ChatClient.getInstance().chatManager().getMessage(it) })
    }

    override fun refreshMessage(message: ChatMessage?) {
        listScrollController.refreshMessage(message)
    }

    override fun removeMessage(message: ChatMessage?) {
        viewModel?.removeMessage(message,
            (loadDataType == ChatUIKitLoadDataType.THREAD || loadDataType == ChatUIKitLoadDataType.ROAM)
                    && message?.status() == ChatMessageStatus.SUCCESS)
    }

    override fun moveToTarget(position: Int) {
        listScrollController.smoothScrollToPosition(position)
    }

    override fun moveToTarget(message: ChatMessage?) {
        if (message == null || messagesAdapter == null || messagesAdapter?.mData == null) {
            ChatLog.e(TAG, "moveToTarget failed: message is null or messageAdapter is null")
            return
        }
        val position = messagesAdapter?.mData?.indexOfFirst {
            it.msgId == message.msgId
        } ?: -1
        if (position >= 0) {
            listScrollController.scrollToTargetMessage(position) {
                highlightTarget(position)
            }
        } else {
            messagesAdapter?.mData?.get(0)?.msgId?.let { msgId ->
                viewModel?.loadMoreRetrievalsMessages(msgId, 100)
                listScrollController.setTargetScrollMsgId(message.msgId)
            }
        }
    }

    override fun highlightTarget(position: Int) {
        messagesAdapter?.highlightItem(position)
    }

    override fun setRefreshing(refreshing: Boolean) {
        binding.messagesRefresh.setEnableRefresh(refreshing)
    }

    override fun isNeedScrollToBottomWhenViewChange(isNeedToScrollBottom: Boolean) {
        listScrollController.setNeedScrollToBottomWhenViewChange(isNeedToScrollBottom)
    }

    override fun addHeaderAdapter(adapter: RecyclerView.Adapter<*>?) {
        adapter?.let {
            concatAdapter.addAdapter(0, it)
        }
    }

    override fun addFooterAdapter(adapter: RecyclerView.Adapter<*>?) {
        adapter?.let {
            concatAdapter.addAdapter(it)
        }
    }

    override fun removeAdapter(adapter: RecyclerView.Adapter<*>?) {
        adapter?.let {
            concatAdapter.removeAdapter(it)
        }
    }

    override fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        binding.messageList.addItemDecoration(decor)
    }

    override fun removeItemDecoration(decor: RecyclerView.ItemDecoration) {
        binding.messageList.removeItemDecoration(decor)
    }

    override fun joinChatRoomSuccess(value: Chatroom?) {

    }

    override fun joinChatRoomFail(error: Int, errorMsg: String?) {
        chatErrorListener?.onChatError(error, errorMsg)
    }

    override fun leaveChatRoomSuccess() {
        loadMessages()
    }

    override fun leaveChatRoomFail(error: Int, errorMsg: String?) {
        chatErrorListener?.onChatError(error, errorMsg)
    }

    override fun getAllMessagesSuccess(messages: List<ChatMessage>) {
        listScrollController.refreshMessages(messages)
    }

    override fun getAllMessagesFail(error: Int, errorMsg: String?) {
        chatErrorListener?.onChatError(error, errorMsg)
    }

    override fun loadLocalMessagesSuccess(messages: List<ChatMessage>) {
        finishRefresh()
        viewModel?.getAllCacheMessages()
        listScrollController.scrollToBottom()
    }

    override fun loadLocalMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadMoreLocalMessagesSuccess(messages: List<ChatMessage>) {
        finishRefresh()
        if (messages.isNotEmpty()) {
            messagesAdapter?.let {
                it.addData(0, messages.toMutableList(), false)
                it.notifyItemRangeInserted(0, messages.size)
            }
        }
    }

    override fun loadMoreLocalMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun fetchRoamMessagesSuccess(messages: List<ChatMessage>) {
        finishRefresh()
        if (loadDataType == ChatUIKitLoadDataType.THREAD){
            if (messages.size < (viewModel?.pageSize ?: 10)){
                enableLoadMore(false)
            }
            if (messages.isNotEmpty()){
                listScrollController.refreshMessages(messages)
            }
        }else{
            viewModel?.getAllCacheMessages()
            listScrollController.scrollToBottom()
        }
    }

    override fun fetchRoamMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun fetchMoreRoamMessagesSuccess(messages: List<ChatMessage>) {
        finishRefresh()
        if (messages.isNotEmpty()) {
            if (loadDataType == ChatUIKitLoadDataType.THREAD) {
                if (messages.size < (viewModel?.pageSize ?: 10)){
                    enableLoadMore(false)
                }
            }

            messagesAdapter?.let {
                val targetPosition = it.itemCount
                viewModel?.getAllCacheMessages()
                listScrollController.scrollToPosition(targetPosition)
            }

        }
    }

    override fun fetchMoreRoamMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadLocalHistoryMessagesSuccess(messages: List<ChatMessage>, direction: ChatSearchDirection) {
        finishRefresh()
        if (direction == ChatSearchDirection.UP) {
            messagesAdapter?.addData(0, messages.toMutableList())
            if (messages.isNotEmpty()) {
                listScrollController.scrollToPosition(messages.size - 1)
            }
        } else {
            messagesAdapter?.let {
                it.addData(messages.toMutableList())
            }
            if (messages.isEmpty() || messages.size < (viewModel?.pageSize ?: 10)) {
                enableLoadMore(false)
                isSearchLatestMessages = true
            }
        }
    }

    override fun loadLocalHistoryMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadMoreRetrievalsMessagesSuccess(messages: List<ChatMessage>) {
        viewModel?.getAllCacheMessages()
        listScrollController.scrollToTargetMessage {
            highlightTarget(it)
        }
    }

    override fun removeMessageSuccess(message: ChatMessage?) {
        viewModel?.getAllCacheMessages()
        //listController.removeMessage(message)
    }

    override fun removeMessageFail(error: Int, errorMsg: String?) {
        chatErrorListener?.onChatError(error, errorMsg)
    }

    enum class ShowType {
        /**
         * Receive messages are located at the start side and sending messages are located at the end side.
         */
        NORMAL,

        /**
         * Receive and sending messages are located at the start side.
         */
        ALL_START
    }

    companion object {
        private val TAG = ChatUIKitMessageListLayout::class.java.simpleName
    }

}



