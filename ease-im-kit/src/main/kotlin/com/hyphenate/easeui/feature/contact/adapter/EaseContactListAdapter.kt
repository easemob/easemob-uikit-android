package com.hyphenate.easeui.feature.contact.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.enums.EaseListViewType
import com.hyphenate.easeui.feature.contact.viewholders.ContactViewHolder
import com.hyphenate.easeui.feature.contact.item.EaseUserContactItem
import com.hyphenate.easeui.feature.group.viewholder.EaseSelectContactViewHolder
import com.hyphenate.easeui.feature.search.interfaces.OnContactSelectListener
import com.hyphenate.easeui.interfaces.OnUserListItemClickListener
import com.hyphenate.easeui.model.EaseUser
import java.util.concurrent.ConcurrentHashMap

open class EaseContactListAdapter: EaseBaseRecyclerViewAdapter<EaseUser>() {
    private var viewType: EaseListViewType = EaseListViewType.LIST_CONTACT
    private var itemClickListener: OnUserListItemClickListener? = null
    private var selectedListener: OnContactSelectListener? = null
    private var userPresence: ConcurrentHashMap<String, ChatPresence>? = null
    private var isShowInitLetter:Boolean = true
    private var selectedMember:MutableList<String> = mutableListOf()

    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<EaseUser> {
        return EaseListViewHolderFactory.createViewHolder(
            LayoutInflater.from(parent.context),parent, EaseListViewType.fromCode(viewType))
    }

    override fun getItemNotEmptyViewType(position: Int): Int {
        return viewType.code
    }

    override fun onBindViewHolder(holder: ViewHolder<EaseUser>, position: Int) {
        if ((holder.itemView) is EaseUserContactItem){
            userPresence?.let {
                ((holder.itemView) as EaseUserContactItem).setUserPresences(it)
            }
        }
        if (holder is ContactViewHolder){
            holder.setShowInitialLetter(isShowInitLetter)
        }

        if (holder is EaseSelectContactViewHolder){
            holder.setSelectedMembers(selectedMember)
        }

        super.onBindViewHolder(holder, position)
        if ((holder.itemView) is EaseUserContactItem){
            itemClickListener?.let {
                ((holder.itemView) as EaseUserContactItem).setOnUserListItemClickListener(it)
            }
        }
        if (holder is EaseSelectContactViewHolder){
            selectedListener?.let {
                holder.setCheckBoxSelectListener(it)
            }
        }
    }

    fun setListViewItemType(viewType: EaseListViewType){
        this.viewType = viewType
        notifyDataSetChanged()
    }

    fun setUserPresences(userPresence: ConcurrentHashMap<String, ChatPresence>?) {
        this.userPresence = userPresence
        notifyDataSetChanged()
    }

    fun setShowInitialLetter(isShow:Boolean){
        this.isShowInitLetter = isShow
    }

    fun setSelectedMembers(selectedMember:MutableList<String>){
        this.selectedMember = selectedMember
        notifyDataSetChanged()
    }

    /**
     * Set message item click listeners.
     * @param listener
     */
    fun setOnUserListItemClickListener(listener: OnUserListItemClickListener) {
        this.itemClickListener = listener
    }


    fun setCheckBoxSelectListener(listener: OnContactSelectListener){
        this.selectedListener = listener
    }



}