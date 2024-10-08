package com.hyphenate.easeui.feature.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.extensions.hasRoute
import com.hyphenate.easeui.databinding.EaseActivityBlockListBinding

open class EaseBlockListActivity: EaseBaseActivity<EaseActivityBlockListBinding>() {
    private var fragment: EaseBlockListFragment? = null
    override fun getViewBinding(inflater: LayoutInflater): EaseActivityBlockListBinding {
       return EaseActivityBlockListBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = EaseBlockListFragment.Builder()
            .useTitleBar(true)
            .useSearchBar(true)
            .enableTitleBarPressBack(true)
            .setEmptyLayout(R.layout.ease_layout_no_data_show_nothing)
        setChildSettings(builder)
        fragment = builder.build()
        fragment?.let { fragment ->
            supportFragmentManager.beginTransaction().replace(binding.flFragment.id, fragment, getFragmentTag()).commit()
        }
    }

    protected open fun setChildSettings(builder: EaseBlockListFragment.Builder) {}

    protected open fun getFragmentTag(): String {
        return "ease_block_fragment"
    }

    companion object {
        fun actionStart(context: Context) {
            Intent(context, EaseBlockListActivity::class.java).apply {
                EaseIM.getCustomActivityRoute()?.getActivityRoute(this.clone() as Intent)?.let {
                    if (it.hasRoute()) {
                        context.startActivity(it)
                        return
                    }
                }
                context.startActivity(this)
            }
        }
    }
}