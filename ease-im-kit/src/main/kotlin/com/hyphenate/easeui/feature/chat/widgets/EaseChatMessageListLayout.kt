package com.hyphenate.easeui.feature.chat.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
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
import com.hyphenate.easeui.common.ChatSearchDirection
import com.hyphenate.easeui.common.Chatroom
import com.hyphenate.easeui.common.RefreshHeader
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.extensions.get
import com.hyphenate.easeui.common.extensions.lifecycleScope
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.impl.OnItemClickListenerImpl
import com.hyphenate.easeui.databinding.EaseChatMessageListBinding
import com.hyphenate.easeui.feature.chat.enums.EaseChatType
import com.hyphenate.easeui.feature.chat.enums.EaseLoadDataType
import com.hyphenate.easeui.feature.chat.adapter.EaseMessagesAdapter
import com.hyphenate.easeui.feature.chat.config.EaseChatMessageItemConfig
import com.hyphenate.easeui.feature.chat.controllers.EaseChatMessageListScrollAndDataController
import com.hyphenate.easeui.feature.chat.enums.getConversationType
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageItemStyle
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageListLayout
import com.hyphenate.easeui.feature.chat.interfaces.IChatMessageListResultView
import com.hyphenate.easeui.feature.chat.interfaces.OnChatErrorListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListItemClickListener
import com.hyphenate.easeui.feature.chat.interfaces.OnMessageListTouchListener
import com.hyphenate.easeui.feature.chat.enums.isShouldStackFromEnd
import com.hyphenate.easeui.feature.chat.reply.interfaces.OnMessageReplyViewClickListener
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.model.EaseMessage
import com.hyphenate.easeui.viewmodel.messages.EaseMessageListViewModel
import com.hyphenate.easeui.viewmodel.messages.IChatMessageListRequest
import com.hyphenate.easeui.widget.EaseImageView.ShapeType
import com.hyphenate.easeui.widget.RefreshLayout
import kotlinx.coroutines.launch

class EaseChatMessageListLayout @JvmOverloads constructor(
    private val context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), IChatMessageListLayout,
    IChatMessageItemStyle, IChatMessageListResultView {

    private val binding: EaseChatMessageListBinding by lazy {
        EaseChatMessageListBinding.inflate(LayoutInflater.from(context), this, true)
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

    private val listController: EaseChatMessageListScrollAndDataController by lazy {
        EaseChatMessageListScrollAndDataController(binding.messageList, messagesAdapter!!, context)
    }

    /**
     * 加载数据的模式
     */
    private var loadDataType: EaseLoadDataType = EaseLoadDataType.LOCAL

    /**
     * The configuration to set the style of the message item.
     */
    private lateinit var itemConfig: EaseChatMessageItemConfig

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
    private var messagesAdapter: EaseMessagesAdapter? = null

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
     * The error listener in chat message list.
     */
    private var chatErrorListener: OnChatErrorListener? = null

    private var baseSearchMessageId: String? = null


    init {
        initAttrs(context, attrs)
        initViews()
        initListener()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        itemConfig = EaseChatMessageItemConfig(context, attrs)
    }

    private fun initViews() {
        if (viewModel == null) {
            viewModel = if (context is AppCompatActivity) {
                ViewModelProvider(context)[EaseMessageListViewModel::class.java]
            } else {
                EaseMessageListViewModel()
            }
        }
        viewModel?.attachView(this)

        binding.messageList.layoutManager = EaseCustomLayoutManager(context)
        messagesAdapter = EaseMessagesAdapter(itemConfig)
        messagesAdapter?.setHasStableIds(true)
        concatAdapter.addAdapter(messagesAdapter!!)
        binding.messageList.adapter = concatAdapter

        if (binding.messagesRefresh.refreshHeader == null) {
            binding.messagesRefresh.setRefreshHeader(RefreshHeader(context))
        }

        // Set not enable to load more.
        binding.messagesRefresh.setEnableLoadMore(false)
    }

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
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!this@EaseChatMessageListLayout.binding.messageList.canScrollVertically(1)) {
                        messageTouchListener?.onReachBottom()
                    }
                } else {
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
    }

    private fun setAdapterListener() {
        messagesAdapter?.run {
            setOnItemClickListener(OnItemClickListenerImpl{ view, position ->
                // Add touch listener
                messageTouchListener?.onTouchItemOutside(view, position)
            })
            setOnMessageListItemClickListener(itemMessageClickListener)
            setOnMessageReplyViewClickListener(messageReplyViewClickListener)
        }
    }

    @JvmOverloads
    fun init(conversationId: String?, chatType: EaseChatType?
             , loadDataType: EaseLoadDataType = EaseLoadDataType.LOCAL) {
        this.loadDataType = loadDataType
        conversation = ChatClient.getInstance().chatManager().getConversation(conversationId
            , chatType?.getConversationType(), true, loadDataType == EaseLoadDataType.THREAD)

        viewModel?.setupWithConversation(conversation)

        // If it is chat thread, no more data can be loaded when the layout is pulled down.
        if (loadDataType == EaseLoadDataType.THREAD) {
            binding.messagesRefresh.setEnableRefresh(false)
        }

        if (loadDataType == EaseLoadDataType.THREAD || loadDataType == EaseLoadDataType.HISTORY) {
            binding.messagesRefresh.setEnableLoadMore(true)
        }

        // Set whether load data from top or from bottom.
        setListStackFromEnd()
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
                EaseLoadDataType.ROAM -> viewModel?.fetchRoamMessages()
                EaseLoadDataType.HISTORY -> viewModel?.loadLocalHistoryMessages(baseSearchMessageId, ChatSearchDirection.DOWN)
                EaseLoadDataType.THREAD -> viewModel?.fetchRoamMessages(direction = ChatSearchDirection.DOWN)
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
        EaseFlowBus.withStick<EaseEvent>(EaseEvent.EVENT.UPDATE.name)
            .post(context.mainScope()
                , EaseEvent(
                    EaseEvent.EVENT.UPDATE.name
                    , EaseEvent.TYPE.CONVERSATION, conversationId)
            )
    }

    private fun loadMorePreviousData() {
        if (loadDataType == EaseLoadDataType.THREAD) return
        val firstMsgId = messagesAdapter?.mData?.first()?.getMessage()?.msgId
        when (loadDataType) {
            EaseLoadDataType.ROAM -> {
                viewModel?.fetchMoreRoamMessages(firstMsgId)
            }
            EaseLoadDataType.HISTORY -> {
                viewModel?.loadLocalHistoryMessages(firstMsgId, ChatSearchDirection.UP)
            }
            else -> {
                viewModel?.loadMoreLocalMessages(firstMsgId)
            }
        }
    }

    private fun loadMoreNewerData() {
        val lastMsgId = messagesAdapter?.mData?.last()?.getMessage()?.msgId
        if (loadDataType == EaseLoadDataType.HISTORY) {
            viewModel?.loadLocalHistoryMessages(lastMsgId, ChatSearchDirection.DOWN)
        } else if (loadDataType == EaseLoadDataType.THREAD) {
            viewModel?.fetchMoreRoamMessages(lastMsgId, ChatSearchDirection.DOWN)
        }
    }

    private fun setListStackFromEnd() {
        binding.messageList.layoutManager?.let {
            if (it is LinearLayoutManager) {
                it.stackFromEnd = loadDataType.isShouldStackFromEnd()
            }
            if (it is EaseCustomLayoutManager) {
                it.setIsStackFromEnd(loadDataType.isShouldStackFromEnd())
            }
        }
    }

    override val currentConversation: ChatConversation?
        get() = this.conversation

    private fun isChatroom(conv: ChatConversation): Boolean {
        return conv.type == ChatConversationType.ChatRoom && loadDataType != EaseLoadDataType.THREAD
    }

    fun isGroupChat(conv: ChatConversation): Boolean {
        return conv.type == ChatConversationType.GroupChat && loadDataType != EaseLoadDataType.THREAD
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

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setAvatarDefaultSrc(src: Drawable?) {
        itemConfig.avatarSrc = src
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setAvatarShapeType(shapeType: ShapeType) {
        itemConfig.avatarConfig.avatarShape = shapeType
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun showNickname(showNickname: Boolean) {
        itemConfig.showNickname = showNickname
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemSenderBackground(bgDrawable: Drawable?) {
        itemConfig.senderBackground = bgDrawable
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemReceiverBackground(bgDrawable: Drawable?) {
        itemConfig.receiverBackground = bgDrawable
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemTextSize(textSize: Int) {
        itemConfig.textSize = textSize
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemTextColor(textColor: Int) {
        itemConfig.textColor = textColor
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeTextSize(textSize: Int) {
        itemConfig.timeTextSize = textSize
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeTextColor(textColor: Int) {
        itemConfig.timeTextColor = textColor
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setTimeBackground(bgDrawable: Drawable?) {
        itemConfig.timeBackground = bgDrawable
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun setItemShowType(type: ShowType) {
        itemConfig.showType = type
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
     * to refresh the list.
     */
    override fun hideChatReceiveAvatar(hide: Boolean) {
        itemConfig.hideReceiverAvatar = hide
    }

    /**
     * After setting the parameter, should call the method [EaseChatMessageListLayout.notifyDataSetChanged]
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

    override fun setMessagesAdapter(adapter: EaseMessagesAdapter?) {
        adapter?.let {
            if (this.messagesAdapter != null && concatAdapter.adapters
                    .contains(this.messagesAdapter)
            ) {
                val index: Int = concatAdapter.adapters.indexOf(this.messagesAdapter)
                concatAdapter.removeAdapter(messagesAdapter!!)
                concatAdapter.addAdapter(index, it)
            } else {
                concatAdapter.addAdapter(it)
            }
            this.messagesAdapter = it
            setAdapterListener()
        }
    }

    override fun getMessagesAdapter(): EaseMessagesAdapter? {
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
        viewModel?.getAllCacheMessages()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        messagesAdapter?.setItemConfig(itemConfig)
        lifecycleScope.launch {
            messagesAdapter?.notifyDataSetChanged()
        }
    }

    override fun refreshToLatest() {
        viewModel?.getAllCacheMessages()
        listController.scrollToBottom()
    }

    override fun refreshMessage(messageId: String?) {
        refreshMessage(messageId?.let { ChatClient.getInstance().chatManager().getMessage(it) })
    }

    override fun refreshMessage(message: ChatMessage?) {
        listController.refreshMessage(message?.get())
    }

    override fun removeMessage(message: ChatMessage?) {
        viewModel?.removeMessage(message?.get())
    }

    override fun moveToTarget(position: Int) {
        listController.smoothScrollToPosition(position)
    }

    override fun moveToTarget(message: ChatMessage?) {
        if (message == null || messagesAdapter == null || messagesAdapter?.mData == null) {
            ChatLog.e(TAG, "moveToTarget failed: message is null or messageAdapter is null")
            return
        }
        val position = messagesAdapter?.mData?.indexOfFirst {
            it.getMessage().msgId == message.msgId
        } ?: -1
        if (position >= 0) {
            listController.scrollToTargetMessage(position) {
                highlightTarget(position)
            }
        } else {
            messagesAdapter?.mData?.get(0)?.getMessage()?.msgId?.let { msgId ->
                viewModel?.loadMoreRetrievalsMessages(msgId, 100)
                listController.setTargetScrollMsgId(message.msgId)
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
        listController.setNeedScrollToBottomWhenViewChange(isNeedToScrollBottom)
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

    override fun getAllMessagesSuccess(messages: List<EaseMessage>) {
        listController.refreshMessages(messages)
    }

    override fun getAllMessagesFail(error: Int, errorMsg: String?) {
        chatErrorListener?.onChatError(error, errorMsg)
    }

    override fun loadLocalMessagesSuccess(messages: List<EaseMessage>) {
        finishRefresh()
        viewModel?.getAllCacheMessages()
        listController.scrollToBottom()
    }

    override fun loadLocalMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadMoreLocalMessagesSuccess(messages: List<EaseMessage>) {
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

    override fun fetchRoamMessagesSuccess(messages: List<EaseMessage>) {
        finishRefresh()
        viewModel?.getAllCacheMessages()
        listController.scrollToBottom()
    }

    override fun fetchRoamMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun fetchMoreRoamMessagesSuccess(messages: List<EaseMessage>) {
        finishRefresh()
        if (messages.isNotEmpty()) {
            viewModel?.getAllCacheMessages()
            listController.smoothScrollToPosition(messages.size - 1)
        }
    }

    override fun fetchMoreRoamMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadLocalHistoryMessagesSuccess(messages: List<EaseMessage>, direction: ChatSearchDirection) {
        finishRefresh()
        if (direction == ChatSearchDirection.UP) {
            messagesAdapter?.addData(0, messages.toMutableList())
            if (messages.isNotEmpty()) {
                listController.smoothScrollToPosition(messages.size - 1)
            }
        } else {
            messagesAdapter?.let {
                it.addData(messages.toMutableList())
                it.mData?.let { list ->
                    listController.scrollToPosition(list.size - 1)
                }
            }
        }
    }

    override fun loadLocalHistoryMessagesFail(error: Int, errorMsg: String?) {
        finishRefresh()
    }

    override fun loadMoreRetrievalsMessagesSuccess(messages: List<EaseMessage>) {
        viewModel?.getAllCacheMessages()
        listController.scrollToTargetMessage {
            highlightTarget(it)
        }
    }

    override fun removeMessageSuccess(message: EaseMessage?) {
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
        private val TAG = EaseChatMessageListLayout::class.java.simpleName
    }

}



