package com.hyphenate.easeui.feature.group.adapter

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.extensions.toProfile
import com.hyphenate.easeui.databinding.EaseLayoutGroupMemberSelectItemBinding
import com.hyphenate.easeui.interfaces.OnContactSelectedListener
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.provider.getSyncMemberProfile

class EaseGroupSelectListAdapter(
    private val groupId: String?
): EaseBaseRecyclerViewAdapter<EaseUser>() {

    companion object{
        private var checkedList:MutableList<String> = mutableListOf()
    }

    private var selectedListener: OnContactSelectedListener?=null
    private var memberList:MutableList<String> = mutableListOf()
    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<EaseUser> =
        GroupSelectListViewHolder(
            EaseLayoutGroupMemberSelectItemBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun onBindViewHolder(holder: ViewHolder<EaseUser>, position: Int) {
        if (holder is GroupSelectListViewHolder){
            selectedListener?.let {
                holder.setCheckBoxSelectListener(it)
            }
            if (memberList.isNotEmpty()){
                holder.setMemberList(memberList)
            }
        }
        super.onBindViewHolder(holder, position)
    }

    fun setCheckBoxSelectListener(listener: OnContactSelectedListener){
        this.selectedListener = listener
        notifyDataSetChanged()
    }

    fun setGroupMemberList(list: MutableList<String>){
        this.memberList = list
        notifyDataSetChanged()
    }

    fun resetSelect(){
        checkedList.clear()
        notifyDataSetChanged()
    }

    inner class GroupSelectListViewHolder(
        private val mViewBinding:EaseLayoutGroupMemberSelectItemBinding
    ): ViewHolder<EaseUser>(binding = mViewBinding) {
        private var selectedListener: OnContactSelectedListener?=null
        private var memberList:MutableList<String> = mutableListOf()

        override fun initView(viewBinding: ViewBinding?) {
            super.initView(viewBinding)
        }

        override fun setData(item: EaseUser?, position: Int) {
            item?.run {
                with(mViewBinding) {

                    memberList.let {
                        cbSelect.isSelected = it.contains(item.userId)
                    }

                    cbSelect.isChecked = checkedList.isNotEmpty() && isContains(checkedList,item.userId)

                    itemLayout.setOnClickListener{ view->
                        if (!memberList.contains(item.userId)){
                            val isChecked = cbSelect.isChecked
                            cbSelect.isChecked = !isChecked
                        }
                    }

                    cbSelect.setOnCheckedChangeListener{ view,isChecked->
                        item.let { u->
                            if (isChecked) {
                                checkedList.let { data ->
                                    if (!data.contains(u.userId)){
                                        data.add(u.userId)
                                    } else { }
                                }
                            }else{
                                checkedList.remove(u.userId)
                            }
                        }
                        selectedListener?.onContactSelectedChanged(view,checkedList)
                    }

                    emPresence.setPresenceData(item.toProfile())
                    tvName.text = item.nickname ?: item.userId

                    letterHeader.visibility = View.GONE
                    if (position == 0 || initialLetter != null && adapter is EaseGroupSelectListAdapter
                        && initialLetter != (adapter as EaseGroupSelectListAdapter).getItem(position - 1)?.initialLetter) {
                        if (!TextUtils.isEmpty(initialLetter)) {
                            letterHeader.visibility = View.VISIBLE
                            letterHeader.text = initialLetter
                        }
                    }
                    EaseIM.getGroupMemberProfileProvider()?.getSyncMemberProfile(groupId, userId)?.let { profile ->
                        tvName.text = profile.name
                    }

                }
            }
        }

        private fun isContains(data: MutableList<String>?, username: String): Boolean {
            return data != null && data.contains(username)
        }

        fun setCheckBoxSelectListener(listener: OnContactSelectedListener){
            this.selectedListener = listener
        }

        fun setMemberList(list: MutableList<String>){
            this.memberList = list
        }

    }
}