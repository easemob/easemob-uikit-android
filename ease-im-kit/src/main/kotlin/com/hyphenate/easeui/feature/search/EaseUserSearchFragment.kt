package com.hyphenate.easeui.feature.search

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.base.EaseBaseSearchFragment
import com.hyphenate.easeui.feature.search.adapter.EaseSearchUserAdapter
import com.hyphenate.easeui.feature.search.interfaces.IEaseSearchResultView
import com.hyphenate.easeui.feature.search.interfaces.OnSearchUserItemClickListener
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.viewmodel.search.EaseSearchViewModel
import com.hyphenate.easeui.viewmodel.search.IEaseSearchRequest

open class EaseUserSearchFragment: EaseBaseSearchFragment<EaseUser>(),IEaseSearchResultView {

    private var searchViewModel: IEaseSearchRequest? = null
    private var query:String? = null
    private var data:MutableList<EaseUser> = mutableListOf()
    private var onCancelListener:OnCancelClickListener?=null
    private var itemClickListener: OnSearchUserItemClickListener? = null

    override fun initViewModel() {
        super.initViewModel()
        searchViewModel = ViewModelProvider(context as AppCompatActivity)[EaseSearchViewModel::class.java]
        searchViewModel?.attachView(this)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

        arguments?.run {
            binding?.run {
                tvRight.visibility = if (getBoolean(Constant.KEY_SHOW_RIGHT_CANCEL, false)) View.VISIBLE else View.GONE
            }
        }
        binding?.etSearch?.requestFocus()
    }

    override fun initRecyclerView(): RecyclerView? {
        return binding?.rvList
    }

    override fun initAdapter(): EaseBaseRecyclerViewAdapter<EaseUser> {
        return EaseSearchUserAdapter()
    }

    override fun searchText(query: String) {
        if (!mListAdapter.data.isNullOrEmpty()){
            mListAdapter.clearData()
        }

        if (!TextUtils.isEmpty(query)) {
            if (mListAdapter is EaseSearchUserAdapter){
                (mListAdapter as EaseSearchUserAdapter).searchText(query)
            }
            query.let { content->
                searchViewModel?.searchUser(content)
            }
        }
    }

    override fun onItemClick(view: View?, position: Int) {
        super.onItemClick(view, position)
        if (position < data.size){
            itemClickListener?.onSearchItemClick(view,position,data[position])
        }
    }

    override fun refreshData() {
        if (!query.isNullOrEmpty()){
            query?.let {
                searchViewModel?.searchUser(it)
            }
        }
    }

    override fun onTvRightClick(view: View) {
        onCancelListener?.onCancelClick(view)
    }

    override fun searchSuccess(result: Any) {
        finishRefresh()
        if (result is MutableList<*>) {
            this.data = result as MutableList<EaseUser>
            mListAdapter.setData(result)
        }
    }

    interface OnCancelClickListener{
        fun onCancelClick(view:View)
    }

    private fun setItemClickListener(itemClickListener: OnSearchUserItemClickListener?) {
        this.itemClickListener = itemClickListener
    }

    private fun setOnCancelListener(onCancelListener: OnCancelClickListener?) {
        this.onCancelListener = onCancelListener
    }


    class Builder {
        private val bundle: Bundle = Bundle()
        private var customFragment: EaseUserSearchFragment? = null
        private var itemClickListener: OnSearchUserItemClickListener? = null
        private var onCancelListener:OnCancelClickListener?=null

        fun <T : EaseUserSearchFragment?> setCustomFragment(fragment: T): Builder {
            customFragment = fragment
            return this
        }

        fun showRightCancel(showCancel: Boolean): Builder {
            bundle.putBoolean(Constant.KEY_SHOW_RIGHT_CANCEL, showCancel)
            return this
        }

        fun setItemClickListener(itemClickListener: OnSearchUserItemClickListener): Builder {
            this.itemClickListener = itemClickListener
            return this
        }

        fun setOnCancelListener(onCancelListener: OnCancelClickListener?): Builder {
            this.onCancelListener = onCancelListener
            return this
        }

        fun build(): EaseUserSearchFragment {
            val fragment =
                if (customFragment != null) customFragment else EaseUserSearchFragment()
            fragment!!.arguments = bundle
            fragment.setItemClickListener(itemClickListener)
            fragment.setOnCancelListener(onCancelListener)
            return fragment
        }
    }

    private object Constant {
        const val KEY_SHOW_RIGHT_CANCEL = "key_show_right_cancel"
    }

}