package com.hyphenate.easeui.feature.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.extensions.hasRoute
import com.hyphenate.easeui.databinding.EaseActivitySearchLayoutBinding
import com.hyphenate.easeui.feature.search.interfaces.OnSearchUserItemClickListener
import com.hyphenate.easeui.interfaces.OnContactSelectedListener
import com.hyphenate.easeui.model.EaseUser

class EaseSearchActivity:EaseBaseActivity<EaseActivitySearchLayoutBinding>(),
    EaseUserSearchFragment.OnCancelClickListener {

    private var searchType: EaseSearchType = EaseSearchType.USER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            searchType = it.getIntExtra(Constant.KEY_SEARCH_TYPE, EaseSearchType.USER.ordinal).let { type->
                when(type){
                    EaseSearchType.USER.ordinal -> { EaseSearchType.USER }
                    EaseSearchType.SELECT_USER.ordinal -> { EaseSearchType.SELECT_USER }
                    EaseSearchType.CONVERSATION.ordinal -> { EaseSearchType.CONVERSATION }
                    else -> { EaseSearchType.CONVERSATION}
                }
            }
        }
        replaceSearchUserFragment()
    }

    override fun getViewBinding(inflater: LayoutInflater): EaseActivitySearchLayoutBinding {
        return EaseActivitySearchLayoutBinding.inflate(inflater)
    }

    private fun replaceSearchUserFragment() {
        val fragment = when (searchType) {
            EaseSearchType.USER -> {
                EaseUserSearchFragment.Builder()
                    .setItemClickListener(object : OnSearchUserItemClickListener{
                        override fun onSearchItemClick(view: View?, position: Int, user: EaseUser) {
                            Intent().apply {
                                putExtra(Constant.KEY_USER, user)
                                setResult(RESULT_OK, this)
                                finish()
                            }
                        }
                    })
                    .showRightCancel(true)
                    .setOnCancelListener(object : EaseUserSearchFragment.OnCancelClickListener{
                        override fun onCancelClick(view: View) {
                            finish()
                        }
                    }).build()
            }
            EaseSearchType.SELECT_USER -> {
                EaseUserSelectSearchFragment.Builder()
                    .setOnSelectListener(object : OnContactSelectedListener{
                        override fun onContactSelectedChanged(
                            v: View,
                            selectedMembers: MutableList<String>
                        ) {
                            Intent().apply {
                                putStringArrayListExtra(Constant.KEY_SELECT_USER,ArrayList(selectedMembers))
                                setResult(RESULT_OK, this)
                                finish()
                            }
                        }
                    })
                    .showRightCancel(true)
                    .build()
            }
            else -> {
                EaseSearchConversationFragment()
            }
        }

        val t = supportFragmentManager.beginTransaction()
        t.add(R.id.fl_fragment, fragment, fragment::javaClass.name).show(fragment).commit()

    }

    override fun onCancelClick(view: View) {
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is EaseUserSelectSearchFragment){
                    fragment.resetSelectList()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private object Constant {
        const val KEY_SELECT_USER = "select_user"
        const val KEY_USER = "user"
        const val KEY_SEARCH_TYPE = "searchType"
    }

    companion object {
        fun actionStart(context: Context, searchType: EaseSearchType) {
            Intent(context, EaseSearchActivity::class.java).apply {
                putExtra(Constant.KEY_SEARCH_TYPE, searchType.ordinal)
                context.startActivity(this)
            }
        }

        fun createIntent(context: Context, searchType: EaseSearchType? = null): Intent {
            val intent = Intent(context, EaseSearchActivity::class.java)
            if (searchType != null) {
                intent.putExtra(Constant.KEY_SEARCH_TYPE, searchType.ordinal)
            }
            EaseIM.getCustomActivityRoute()?.getActivityRoute(intent.clone() as Intent)?.let {
                if (it.hasRoute()) {
                    return it
                }
            }
            return intent
        }
    }
}

enum class EaseSearchType{
    USER,
    SELECT_USER,
    CONVERSATION
}