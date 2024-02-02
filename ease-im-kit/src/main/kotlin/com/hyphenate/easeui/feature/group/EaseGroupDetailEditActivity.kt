package com.hyphenate.easeui.feature.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.EaseConstant
import com.hyphenate.easeui.common.extensions.hasRoute
import com.hyphenate.easeui.databinding.EaseLayoutGroupDetailEditBinding
import com.hyphenate.easeui.feature.group.interfaces.IEaseGroupResultView
import com.hyphenate.easeui.interfaces.EaseGroupListener
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.viewmodel.group.EaseGroupViewModel
import com.hyphenate.easeui.viewmodel.group.IGroupRequest

open class EaseGroupDetailEditActivity:EaseBaseActivity<EaseLayoutGroupDetailEditBinding>(),
    IEaseGroupResultView {
    private var type:EditType = EditType.ACTION_EDIT_GROUP_UN_KNOW
    private var groupViewModel: IGroupRequest? = null
    private var groupId:String? = ""
    private var groupNickName:String?=""
    private var group: ChatGroup? = null

    private val groupChangeListener = object : EaseGroupListener() {

        override fun onGroupDestroyed(groupId: String?, groupName: String?) {
            finish()
        }

    }

    override fun getViewBinding(inflater: LayoutInflater): EaseLayoutGroupDetailEditBinding? {
        return EaseLayoutGroupDetailEditBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(KEY_GROUP_EDIT_TYPE, -1)
        groupId = intent.getStringExtra(KEY_GROUP_ID)
        groupId?.let {
            group = ChatClient.getInstance().groupManager().getGroup(it)
        }
        type = EditType.values()[index]
        if (type == EditType.ACTION_EDIT_GROUP_ATTRIBUTE){
            groupNickName = intent.getStringExtra(KEY_GROUP_NICKNAME)
        }

        initView()
        initListener()
    }

    private fun initView(){
        when(type){
            EditType.ACTION_EDIT_GROUP_NAME -> {
                binding.editNameLayout.visibility = View.VISIBLE
                binding.etName.setText(group?.groupName ?: "")
                binding.inputNameCount.text = resources.getString(R.string.ease_group_change_name_count,group?.groupName?.length ?: 0)
                binding.titleBar.setTitle(resources.getString(R.string.ease_group_change_name))
                showSoftInput(binding.etName)
                updateSaveView(binding.etName.text.length)
            }
            EditType.ACTION_EDIT_GROUP_DESCRIBE -> {
                binding.editDescribeLayout.visibility = View.VISIBLE
                binding.etDescribe.setText(group?.description ?: "")
                binding.inputDescribeCount.text = resources.getString(R.string.ease_group_change_describe_count,group?.description?.length ?: 0)
                binding.titleBar.setTitle(resources.getString(R.string.ease_group_change_describe))
                showSoftInput(binding.etDescribe)
                updateSaveView(binding.etDescribe.text.length)
            }
            EditType.ACTION_EDIT_GROUP_ATTRIBUTE -> {
                binding.editAttributeLayout.visibility = View.VISIBLE
                binding.inputAttributeCount.text = resources.getString(R.string.ease_group_change_name_count,groupNickName?.length ?: 0)
                binding.etAttribute.setText(groupNickName ?: "")
                binding.titleBar.setTitle(resources.getString(R.string.ease_group_detail_my_notes))
                groupNickName?.let {
                    binding.etAttribute.setText(it)
                }
                updateSaveView(binding.etAttribute.text.length)
            }
            else -> {

            }
        }
        groupViewModel = ViewModelProvider(this)[EaseGroupViewModel::class.java]
        groupViewModel?.attachView(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EaseIM.removeGroupChangeListener(groupChangeListener)
    }

    private fun initListener(){
        EaseIM.addGroupChangeListener(groupChangeListener)
        binding.titleBar.setNavigationOnClickListener { mContext.onBackPressed() }
        binding.titleBar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.action_save -> {
                    when(type){
                        EditType.ACTION_EDIT_GROUP_NAME -> {
                            groupId?.let {
                                if (binding.etName.text.isNotEmpty()){
                                    groupViewModel?.changeChatGroupName(
                                        groupId = it,
                                        newName = binding.etName.text.trim().toString()
                                    )
                                }
                            }
                        }
                        EditType.ACTION_EDIT_GROUP_DESCRIBE -> {
                            groupId?.let {
                                groupViewModel?.changeChatGroupDescription(
                                    groupId = it,
                                    description = binding.etDescribe.text.trim().toString()
                                )
                            }
                        }
                        EditType.ACTION_EDIT_GROUP_ATTRIBUTE -> {
                            val map = mutableMapOf<String,String>()
                            map[EaseConstant.GROUP_MEMBER_ATTRIBUTE_NICKNAME] = binding.etAttribute.text?.trim().toString()

                            groupId?.let {
                                groupViewModel?.setGroupMemberAttributes(
                                    groupId = it,
                                    userId= ChatClient.getInstance().currentUser,
                                    attribute = map
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
        binding.etName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                val length = s.toString().trim().length
                if (length == 0){
                    binding.inputNameCount.text =
                        resources.getString(R.string.ease_group_change_name_count, 0)
                }else{
                    binding.inputNameCount.text =
                        resources.getString(R.string.ease_group_change_name_count,length)
                }
                updateSaveView(binding.etName.text.length)
            }
        })

        binding.etDescribe.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val length = s.toString().trim().length
                if (length == 0){
                    binding.inputDescribeCount.text =
                        resources.getString(R.string.ease_group_change_describe_count, 0)
                }else{
                    binding.inputDescribeCount.text =
                        resources.getString(R.string.ease_group_change_describe_count,length)
                }

                updateSaveView(binding.etDescribe.text.length)
            }
        })

        binding.etAttribute.addTextChangedListener (object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val length = s.toString().trim().length
                if (length == 0){
                    binding.inputAttributeCount.text =
                        resources.getString(R.string.ease_group_change_name_count,0)
                }else{
                    binding.inputAttributeCount.text =
                        resources.getString(R.string.ease_group_change_name_count,length)
                }
                updateSaveView(binding.etAttribute.text.length)
            }
        })
    }

    open fun updateSaveView(length: Int){
        binding.titleBar.setMenuTitleColor(ContextCompat.getColor(mContext,
            if (length != 0) R.color.ease_color_primary else R.color.ease_color_on_background_high))
    }

    open fun showSoftInput(editText:EditText){
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun changeChatGroupNameSuccess() {
        val group = ChatClient.getInstance().groupManager().getGroup(groupId)
        val convInfo = EaseIM.getCache().getConvInfo(groupId)
        convInfo?.let {
            it.name = group.groupName
        }
        groupId?.let {
            EaseIM.getCache().insertConvInfo(it,convInfo?: EaseProfile(it,group.groupName))
        }
        finish()
    }

    override fun changeChatGroupNameFail(code: Int, error: String) {
        ChatLog.e(TAG,"changeChatGroupNameFail $code $error")
    }

    override fun changeChatGroupDescriptionSuccess() {
        finish()
    }

    override fun changeChatGroupDescriptionFail(code: Int, error: String) {
        ChatLog.e(TAG,"changeChatGroupDescriptionFail $code $error")
    }

    override fun setGroupMemberAttributesSuccess() {
        finish()
    }

    override fun setGroupMemberAttributesFail(code: Int, error: String) {
        ChatLog.e(TAG,"setGroupMemberAttributesFail $code $error")
    }

    companion object {
        private const val TAG = "EaseGroupDetailEditActivity"
        private const val KEY_GROUP_EDIT_TYPE = "edit_type"
        private const val KEY_GROUP_ID = "group_id"
        private const val KEY_GROUP_NICKNAME = "group_nick_name"

        fun createIntent(
            context: Context,
            groupId:String,
            type: EditType,
            nickname:String?="",
        ): Intent {
            val intent = Intent(context, EaseGroupDetailEditActivity::class.java)
            intent.putExtra(KEY_GROUP_EDIT_TYPE, type.ordinal)
            intent.putExtra(KEY_GROUP_ID, groupId)
            nickname?.let {
                intent.putExtra(KEY_GROUP_NICKNAME, it)
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

enum class EditType(var code:Int){
    ACTION_EDIT_GROUP_UN_KNOW(-1),
    ACTION_EDIT_GROUP_NAME(0),
    ACTION_EDIT_GROUP_DESCRIBE(1),
    ACTION_EDIT_GROUP_ATTRIBUTE(2)
}