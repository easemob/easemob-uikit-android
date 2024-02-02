package com.hyphenate.easeui.base

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class EaseBaseSheetFragmentDialog<B : ViewBinding?> : BottomSheetDialogFragment() {

    var binding: B? = null
    private var topOffset = 0
    private var mBehavior: BottomSheetBehavior<*>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = getViewBinding(inflater, container)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onHandleOnBackPressed()
            }
        })
        return this.binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
        dialog?.setOnShowListener { _: DialogInterface? ->
            (view.parent as ViewGroup).setBackgroundColor(Color.TRANSPARENT)
        }
        initView()
        initListener()
        initData()
    }

    open fun initView() {}

    open fun initListener() {}

    open fun initData() {}

    protected fun setOnApplyWindowInsets(view: View) {
        dialog?.window?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it.decorView) { v: View?, insets: WindowInsetsCompat ->
                val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, 0, 0, systemInset.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog?.setCanceledOnTouchOutside(getCanceledOnTouchOutside())
        }
        if (showExpandedState()) {
            val layoutParams = requireView().layoutParams
            layoutParams.height = getHeight()
            mBehavior = BottomSheetBehavior.from<View>(requireView().parent as View)
            mBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
        mBehavior?.isDraggable = isDraggable()
    }

    /**
     * Get fragment's height
     */
    private fun getHeight(): Int {
        return resources.displayMetrics.heightPixels - getTopOffset()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): B?

    open fun onHandleOnBackPressed() {
        dismiss()
    }

    /**
     * Whether can be canceled on touch outside.
     */
    protected open fun getCanceledOnTouchOutside(): Boolean {
        return true
    }

    protected open fun showExpandedState(): Boolean {
        return false
    }

    protected open fun isDraggable():Boolean{
        return true
    }

    /**
     * Get current fragment's top offset.
     */
    protected open fun getTopOffset(): Int {
        return topOffset
    }

    /**
     * Set current fragment's top offset.
     */
    protected open fun setTopOffset(topOffset: Int) {
        this.topOffset = topOffset
    }

    /**
     * Hide the bottom sheet dialog.
     */
    open fun hide() {
        mBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }
}