package com.hyphenate.easeui.base

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.hyphenate.easeui.R

abstract class EaseBaseFragment<B : ViewBinding> : Fragment() {
    var binding: B? = null
    lateinit var mContext: Activity
    private var loadingDialog: AlertDialog? = null

    open fun showLoading(cancelable: Boolean) {
        if (loadingDialog == null) {
            loadingDialog = AlertDialog.Builder(requireActivity()).setView(R.layout.ease_view_base_loading).create().apply {
                window?.decorView?.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        loadingDialog?.setCancelable(cancelable)
        loadingDialog?.show()
    }

    open fun dismissLoading() {
        loadingDialog?.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context as Activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = getViewBinding(inflater, container)
        return this.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initViewModel()
        initListener()
        initData()
    }

    /**
     * Initialize the views
     * @param savedInstanceState
     */
    protected open fun initView(savedInstanceState: Bundle?) {
        Log.d("TAG", "fragment = " + this.javaClass.simpleName)
    }

    /**
     * Initialize the viewModels
     */
    protected open fun initViewModel() {}

    /**
     * Initialize the Data
     */
    protected open fun initData() {}

    /**
     * Initialize the listeners
     */
    protected open fun initListener() {}

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    val parentActivity: EaseBaseActivity<*>
        get() = requireActivity() as EaseBaseActivity<*>

    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): B?

}