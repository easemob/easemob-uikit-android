package com.hyphenate.easeui.feature.group.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.base.EaseBaseListFragment
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.extensions.getOwnerInfo
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.ContactSortedHelper
import com.hyphenate.easeui.feature.group.adapter.EaseGroupMemberListAdapter
import com.hyphenate.easeui.feature.group.interfaces.IEaseGroupResultView
import com.hyphenate.easeui.feature.group.interfaces.IGroupMemberEventListener
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.model.setUserInitialLetter
import com.hyphenate.easeui.viewmodel.group.EaseGroupViewModel
import com.hyphenate.easeui.viewmodel.group.IGroupRequest

class EaseGroupMemberFragment:EaseBaseListFragment<EaseUser>(),IEaseGroupResultView {
    private val memberAdapter: EaseGroupMemberListAdapter by lazy { EaseGroupMemberListAdapter(groupId) }
    private var groupViewModel: IGroupRequest? = null
    private var groupId:String?=null
    private var currentGroup:ChatGroup?=null
    private var sortedList:MutableList<EaseUser> = mutableListOf()
    private var listener:IGroupMemberEventListener?=null

    companion object {
        private const val TAG = "EaseGroupMemberFragment"
        private const val KEY_GROUP_ID = "group_id"
    }

    override fun initView(savedInstanceState: Bundle?) {
        groupId = arguments?.getString(KEY_GROUP_ID) ?: ""

        super.initView(savedInstanceState)

        groupId?.let {
            currentGroup = ChatClient.getInstance().groupManager().getGroup(it)
        }
        groupViewModel = ViewModelProvider(this)[EaseGroupViewModel::class.java]
    }

    override fun initViewModel() {
        super.initViewModel()
        groupViewModel?.attachView(this)
    }

    override fun initListener() {
        super.initListener()
        binding?.rvList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // When scroll to bottom, load more data
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val visibleList = memberAdapter?.mData?.filterIndexed { index, _ ->
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

    override fun initData() {
        super.initData()
        loadData()
    }

    fun loadData(){
        groupId?.let {
            groupViewModel?.fetchGroupMemberFromService(it)
        }
    }

    fun loadLocalData(){
        finishRefresh()
        groupId?.let { groupId->
            sortedList.clear()
            groupViewModel?.loadLocalMember(groupId)
        }
    }

    override fun initRecyclerView(): RecyclerView? {
        return binding?.rvList
    }

    override fun initAdapter(): EaseBaseRecyclerViewAdapter<EaseUser> {
        return memberAdapter
    }

    override fun refreshData() {
        loadLocalData()
    }

    override fun fetchGroupMemberSuccess(user: List<EaseUser>) {
        val data = user.toMutableList()
        finishRefresh()
        groupId?.let {
            val ownerInfo = currentGroup?.getOwnerInfo(it)
            ownerInfo?.let { o->
                o.setUserInitialLetter()
            }
            ownerInfo?.let { it1 -> data.add(it1) }
            sortedList = ContactSortedHelper.sortedList(data).toMutableList()
            memberAdapter.setData(sortedList)
            listener?.onGroupMemberLoadSuccess(sortedList)
        }
    }

    override fun fetchGroupMemberFail(code: Int, error: String) {
        finishRefresh()
        ChatLog.e(TAG,"fetchGroupMemberFail $code $error")
    }

    override fun onItemClick(view: View?, position: Int) {
        sortedList.let {
            if (EaseIM.getCurrentUser()?.id != it[position].userId){
                listener?.onGroupMemberListItemClick(view,it[position])
            }
        }
    }

    fun setOnGroupMemberItemClickListener(listener: IGroupMemberEventListener){
        this.listener = listener
    }

    override fun fetchMemberInfoSuccess(members: Map<String, EaseProfile>?) {
        members?.let { m->
            val data = m.map {
                it.value.toUser()
            }.toMutableList()
            memberAdapter.setData(data)
        }
    }

    override fun fetchMemberInfoFail(code: Int, error: String) {
        ChatLog.e(TAG,"fetchMemberInfoFail $code $error")
    }

    override fun loadLocalMemberSuccess(members: List<EaseUser>) {
        finishRefresh()
        groupId?.let {
            memberAdapter.setData(members.toMutableList())
            listener?.onGroupMemberLoadSuccess(members.toMutableList())
        }
    }

}