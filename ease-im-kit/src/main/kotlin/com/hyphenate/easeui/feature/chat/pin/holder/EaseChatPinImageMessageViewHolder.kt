package com.hyphenate.easeui.feature.chat.pin.holder

import android.annotation.SuppressLint
import android.view.View
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.extensions.getMessageDigest
import com.hyphenate.easeui.databinding.EasePinlistImageLayoutBinding
import com.hyphenate.easeui.provider.getSyncUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class EaseChatPinImageMessageViewHolder (
    private val mItemSubViewListener: EaseBaseRecyclerViewAdapter.OnItemSubViewClickListener?,
    val viewBinding: EasePinlistImageLayoutBinding
): EaseBaseRecyclerViewAdapter.ViewHolder<ChatMessage>(binding = viewBinding) {
    private var isConfirm:Boolean = false
    @SuppressLint("SetTextI18n")
    override fun setData(item: ChatMessage?, position: Int) {
        viewBinding.run {
            item?.let { message->
                tvState.text = itemView.context.getString(R.string. ease_pin_list_item_delete)
                isConfirm = false
                val operatorId = message.pinnedInfo()?.operatorId()
                val pinTime = message.pinnedInfo()?.pinTime()
                val operatorUserInfo = EaseIM.getUserProvider()?.getSyncUser(operatorId)
                val fromUserInfo = EaseIM.getUserProvider()?.getSyncUser(message.from)

                tvFrom.text = "${operatorUserInfo?.getRemarkOrName()?:kotlin.run { operatorId }} " +
                        "pinned ${fromUserInfo?.getRemarkOrName()?:kotlin.run { message.from }} 's message"

                tvContent.text = message.getMessageDigest(itemView.context)

                val sdf = SimpleDateFormat("MM-dd, HH:mm")
                sdf.timeZone = TimeZone.getTimeZone("GMT+8")
                pinTime?.let {
                    val formattedDate = sdf.format(Date(pinTime))
                    tvTime.text = formattedDate
                }

                tvState.setOnClickListener(View.OnClickListener { v ->
                    if (isConfirm){
                        mItemSubViewListener?.onItemSubViewClick(v, position)
                    }else{
                        tvState.text = itemView.context.getString(R.string.ease_pin_list_item_confirm_delete)
                    }
                    isConfirm = !isConfirm
                })
            }
        }
    }
}