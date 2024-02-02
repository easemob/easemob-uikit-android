package com.hyphenate.easeui.feature.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.chat.EMGroup
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.RefreshHeader
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.extensions.hasRoute
import com.hyphenate.easeui.databinding.EaseLayoutGroupListBinding
import com.hyphenate.easeui.feature.group.adapter.EaseGroupListAdapter
import com.hyphenate.easeui.feature.group.interfaces.IEaseGroupResultView
import com.hyphenate.easeui.interfaces.EaseGroupListener
import com.hyphenate.easeui.interfaces.OnItemClickListener
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.viewmodel.group.EaseGroupViewModel
import com.hyphenate.easeui.viewmodel.group.IGroupRequest

class EaseGroupListActivity:EaseBaseActivity<EaseLayoutGroupListBinding>(),
    IEaseGroupResultView, OnItemClickListener {
    private lateinit var adapter:EaseGroupListAdapter
    private var groupViewModel: IGroupRequest? = null
    private var data:MutableList<ChatGroup> = mutableListOf()
    private val layoutManager = LinearLayoutManager(this)
    private var isLoading:Boolean = true
    private var currentPage:Int = 0

    private val groupChangeListener = object : EaseGroupListener() {

        override fun onGroupDestroyed(groupId: String?, groupName: String?) {
            refreshData()
        }

        override fun onAutoAcceptInvitationFromGroup(
            groupId: String?,
            inviter: String?,
            inviteMessage: String?
        ) {
            refreshData()
        }

        override fun onSpecificationChanged(group: EMGroup?) {
            refreshData()
        }

    }

    override fun getViewBinding(inflater: LayoutInflater): EaseLayoutGroupListBinding {
        return EaseLayoutGroupListBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initListener()
        initData()
    }

    fun initView(){
        adapter = EaseGroupListAdapter()

        binding.rvList.layoutManager = layoutManager
        binding.rvList.adapter = adapter

        groupViewModel = ViewModelProvider(this)[EaseGroupViewModel::class.java]
        groupViewModel?.attachView(this)

        // Set refresh layout
        // Can not load more
        binding.refreshLayout.setEnableLoadMore(false)
        val refreshHeader = binding.refreshLayout.refreshHeader
        if (refreshHeader == null) {
            binding.refreshLayout.setRefreshHeader(RefreshHeader(this))
        }
        updateView()
    }

    private fun updateView(){
        binding.titleContact.setTitle(resources.getString(R.string.ease_group_count,data.size))
    }

    fun initListener(){
        EaseIM.addGroupChangeListener(groupChangeListener)
        binding.titleContact.setNavigationOnClickListener {
            finish()
        }
        adapter.setOnItemClickListener(this)
        binding.refreshLayout.setOnRefreshListener {
            refreshData()
        }
        binding.rvList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    currentPage++
                    loadMoreData()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        EaseIM.removeGroupChangeListener(groupChangeListener)
    }

    private fun initData(){
        currentPage = 0
        data.clear()
        adapter.clearData()
        groupViewModel?.loadJoinedGroupData(currentPage)
        initEventBus()
    }

    private fun initEventBus(){
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.DESTROY.name).register(this) {
            if (it.isGroupChange) {
                refreshData()
            }
        }

        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.LEAVE.name).register(this) {
            if (it.isGroupChange) {
                refreshData()
            }
        }

        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.ADD.name).register(this) {
            if (it.isGroupChange) {
                refreshData()
            }
        }
    }

    private fun refreshData(){
        groupViewModel?.loadLocalJoinedGroupData()
    }

    private fun loadMoreData(){
        isLoading = true
        groupViewModel?.loadJoinedGroupData(currentPage)
    }

    override fun loadGroupListSuccess(list: MutableList<ChatGroup>) {
        data.addAll(list)
        adapter.addData(list)
        isLoading = false
        binding.refreshLayout.finishRefresh()
        updateView()
    }

    override fun loadGroupListFail(code: Int, error: String) {
        isLoading = false
        binding.refreshLayout.finishRefresh()
        updateView()
    }

    override fun loadLocalGroupListSuccess(list: MutableList<ChatGroup>) {
        data.clear()
        adapter.clearData()
        currentPage = 0

        list.reverse().apply {
            data.addAll(list)
            adapter.addData(list)
            isLoading = false
            binding.refreshLayout.finishRefresh()
            updateView()
        }
    }

    override fun loadLocalGroupListFail(code: Int, error: String) {
        ChatLog.e(TAG,"loadLocalGroupListFail $code $error")
        isLoading = false
        binding.refreshLayout.finishRefresh()
        updateView()
    }

    override fun onItemClick(view: View?, position: Int) {
        startActivity(EaseGroupDetailActivity.createIntent(this,data[position].groupId))
    }

    companion object {
        private const val TAG = "EaseGroupListActivity"
        fun actionStart(context: Context) {
            val intent = Intent(context, EaseGroupListActivity::class.java)
            EaseIM.getCustomActivityRoute()?.getActivityRoute(intent.clone() as Intent)?.let {
                if (it.hasRoute()) {
                    context.startActivity(it)
                    return
                }
            }
            context.startActivity(intent)
        }
    }


}