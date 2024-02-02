package com.hyphenate.easeui.feature.group.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.base.EaseBaseListFragment
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.SidebarHelper
import com.hyphenate.easeui.feature.group.adapter.EaseGroupSelectListAdapter
import com.hyphenate.easeui.feature.group.interfaces.IEaseGroupResultView
import com.hyphenate.easeui.interfaces.OnContactSelectedListener
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.viewmodel.group.EaseGroupViewModel
import com.hyphenate.easeui.viewmodel.group.IGroupRequest
import com.hyphenate.easeui.widget.EaseSidebar

open class EaseGroupRemoveMemberFragment : EaseBaseListFragment<EaseUser>(), IEaseGroupResultView,
    OnContactSelectedListener {
    private val memberSelectAdapter: EaseGroupSelectListAdapter by lazy { EaseGroupSelectListAdapter(groupId) }
    private var listener:OnContactSelectedListener?=null
    private var groupId:String?=null
    private var groupViewModel: IGroupRequest? = null
    private var data:MutableList<EaseUser> = mutableListOf()
    private var sideBarContact: EaseSidebar?=null

    companion object {
        private const val TAG = "EaseGroupRemoveMemberFragment"
        private const val KEY_GROUP_ID = "group_id"
    }

    override fun initRecyclerView(): RecyclerView? {
        return binding?.rvList
    }

    override fun initAdapter(): EaseBaseRecyclerViewAdapter<EaseUser> {
        return memberSelectAdapter
    }

    override fun refreshData() {
        loadData()
    }

    override fun initData() {
        super.initData()
        loadData()
    }

    fun resetSelect(){
        memberSelectAdapter.resetSelect()
    }

    fun setMemberList(members: MutableList<String>){
        memberSelectAdapter.setGroupMemberList(members)
    }

    open fun loadData(){
        finishRefresh()
        data.clear()
        groupId?.let { groupId->
            groupViewModel?.loadLocalMember(groupId)
        }
    }

    open fun initSideBar(){
        sideBarContact?.visibility = View.VISIBLE
        val sidebarHelper = SidebarHelper()
        sidebarHelper.setupWithRecyclerView(
            binding?.rvList,
            memberSelectAdapter
        )
        sideBarContact?.setOnTouchEventListener(sidebarHelper)
    }

    fun setSideBar(sidebar:EaseSidebar){
        this.sideBarContact = sidebar
    }

    override fun fetchMemberInfoSuccess(members: Map<String, EaseProfile>?) {
        members?.let { m->
            val data = m.map {
                it.value.toUser()
            }.toMutableList()
            memberSelectAdapter.setData(data)
        }
    }

    override fun fetchMemberInfoFail(code: Int, error: String) {
        ChatLog.e(TAG,"fetchMemberInfoFail $code $error")
    }

    override fun initViewModel() {
        super.initViewModel()
        groupViewModel?.attachView(this)
    }

    override fun initView(savedInstanceState: Bundle?) {
        groupId = arguments?.getString(KEY_GROUP_ID) ?: ""
        super.initView(savedInstanceState)
        groupViewModel = ViewModelProvider(this)[EaseGroupViewModel::class.java]
        initSideBar()
    }

    override fun initListener() {
        super.initListener()
        memberSelectAdapter.setCheckBoxSelectListener(this)

        binding?.rvList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // When scroll to bottom, load more data
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val visibleList = memberSelectAdapter.mData?.filterIndexed { index, _ ->
                        index in firstVisibleItemPosition..lastVisibleItemPosition
                    }
                    groupId?.let { id ->
                        val members = mutableMapOf<String,MutableList<String>>()
                        visibleList?.map { user->
                            user.userId
                        }?.let {
                            members[id] = it.toMutableList()
                        }
                        groupViewModel?.fetchMemberInfo(members)
                    }
                }
            }
        })
    }

    override fun onContactSelectedChanged(v: View, selectedMembers: MutableList<String>) {
        listener?.onContactSelectedChanged(v, selectedMembers)
    }

    fun setRemoveSelectListener(listener: OnContactSelectedListener){
        this.listener = listener
    }

    override fun loadLocalMemberSuccess(members: List<EaseUser>) {
        data = members.toMutableList().filter {
            it.userId != EaseIM.getCurrentUser()?.id
        }.toMutableList()
        memberSelectAdapter.setData(data)
    }
}