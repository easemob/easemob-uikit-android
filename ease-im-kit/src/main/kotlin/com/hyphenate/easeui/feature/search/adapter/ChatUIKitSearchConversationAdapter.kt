package com.hyphenate.easeui.feature.search.adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import com.hyphenate.easeui.R
import com.hyphenate.easeui.base.ChatUIKitBaseRecyclerViewAdapter
import com.hyphenate.easeui.common.extensions.getDisplayInfo
import com.hyphenate.easeui.databinding.UikitLayoutGroupSelectContactBinding
import com.hyphenate.easeui.model.ChatUIKitConversation
import com.hyphenate.easeui.model.isChatRoom
import com.hyphenate.easeui.model.isGroupChat

class ChatUIKitSearchConversationAdapter: ChatUIKitBaseRecyclerViewAdapter<ChatUIKitConversation>() {
    private var query : String = ""

    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ChatUIKitConversation> =
        ChatUIKitSearchConversationViewHolder(
            UikitLayoutGroupSelectContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )

    fun searchText(query: String){
        this.query = query
    }

    inner class ChatUIKitSearchConversationViewHolder(private val binding: UikitLayoutGroupSelectContactBinding)
        : ViewHolder<ChatUIKitConversation>(binding = binding) {
        override fun setData(item: ChatUIKitConversation?, position: Int) {
            item?.run {
                with(binding) {
                    cbSelect.visibility = View.GONE

                    // Set conversation avatar and name.
                    val displayInfo = item.getDisplayInfo()
                    val placeholderRes = when {
                        item.isGroupChat() -> R.drawable.uikit_default_group_avatar
                        item.isChatRoom() -> R.drawable.ease_default_chatroom_avatar
                        else -> R.drawable.uikit_default_avatar
                    }
                    emPresence.setUserAvatarData(avatar = placeholderRes, nickname = null)
                    tvName.text = displayInfo.name
                    if (displayInfo.avatar.isNullOrEmpty().not()) {
                        emPresence.getUserAvatar().load(displayInfo.avatar) {
                            placeholder(placeholderRes)
                            error(placeholderRes)
                        }
                    }

                    val title  = tvName.text.toString().trim()
                    val spannableString = SpannableString(title)
                    query.let {
                        val startIndex = title.indexOf(it, ignoreCase = true)
                        if (startIndex != -1) {
                            val endIndex = startIndex + it.length
                            spannableString.setSpan(
                                ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.ease_color_primary)),
                                startIndex, endIndex,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            tvName.text = spannableString
                        }
                    }
                }
            }
        }
    }

}
