package com.hyphenate.easeui.configs

import android.content.Context
import androidx.core.content.ContextCompat
import com.hyphenate.easeui.R
import com.hyphenate.easeui.model.EaseMenuItem
import java.util.Collections

class EaseDetailMenuConfig(
    val context:Context,
    private var contactItemModels: MutableList<EaseMenuItem>? = null,
    private var groupItemModels: MutableList<EaseMenuItem>? = null,
) {
    init {
        if (contactItemModels == null){
            contactItemModels = mutableListOf()
            for (i in defaultContactItemString.indices) {
                registerContactMenuItem(
                    defaultContactItemString[i],
                    defaultContactItemDrawables[i],
                    defaultContactItemIds[i],
                    defaultContactItemColor[i],
                    defaultContactItemVisible[i],
                    defaultContactItemOrder[i]
                )
            }
        }

        if (groupItemModels == null){
            groupItemModels = mutableListOf()
            for (i in defaultGroupItemString.indices) {
                registerGroupMenuItem(
                    defaultGroupItemString[i],
                    defaultGroupItemDrawables[i],
                    defaultGroupItemIds[i],
                    defaultGroupItemColor[i],
                    defaultGroupItemVisible[i],
                    defaultGroupItemOrder[i]
                )
            }
        }

    }

    fun getDefaultContactDetailMenu(): MutableList<EaseMenuItem>?{
        return contactItemModels
    }

    fun getDefaultGroupDetailMenu(): MutableList<EaseMenuItem>?{
        return groupItemModels
    }

    private fun registerContactMenuItem(nameRes: Int, drawableRes: Int, itemId: Int, itemColor: Int, itemVisible: Boolean, order: Int = 0) {
        registerContactMenuItem(context.getString(nameRes), drawableRes, itemId, itemColor, itemVisible, order)
    }

    private fun registerContactMenuItem(name: String?, drawableRes: Int, itemId: Int, itemColor: Int, itemVisible: Boolean, order: Int) {
        val item = EaseMenuItem(
            title = name ?: "",
            resourceId = drawableRes,
            menuId = itemId,
            titleColor = ContextCompat.getColor(context, itemColor),
            isVisible = itemVisible,
            order = order
        )
        contactItemModels?.let {
            it.add(item)
            sortByOrder(it)
        }

    }

    private fun registerGroupMenuItem(nameRes: Int, drawableRes: Int, itemId: Int, itemColor: Int, itemVisible: Boolean, order: Int = 0) {
        registerGroupMenuItem(context.getString(nameRes), drawableRes, itemId, itemColor, itemVisible, order)
    }

    private fun registerGroupMenuItem(name: String?, drawableRes: Int, itemId: Int, itemColor: Int, itemVisible: Boolean, order: Int) {
        val item = EaseMenuItem(
            title = name ?: "",
            resourceId = drawableRes,
            menuId = itemId,
            titleColor = ContextCompat.getColor(context, itemColor),
            isVisible = itemVisible,
            order = order
        )
        groupItemModels?.let {
            it.add(item)
            sortByOrder(it)
        }
    }

    private fun sortByOrder(itemModels: List<EaseMenuItem>) {
        Collections.sort(itemModels) { o1, o2 ->
            val `val` = o1.order - o2.order
            if (`val` > 0) {
                1
            } else if (`val` == 0) {
                0
            } else {
                -1
            }
        }
    }

}

val defaultGroupItemIds = intArrayOf(
    R.id.extend_item_message,
    R.id.extend_item_audio_call,
    R.id.extend_item_video_call,
    R.id.extend_item_search,
)

val defaultGroupItemString = intArrayOf(
    R.string.ease_detail_item_message, R.string.ease_detail_item_audio,
    R.string.ease_detail_item_video,R.string.ease_detail_item_search_msg,
)

val defaultGroupItemDrawables = intArrayOf(
    R.drawable.ease_bubble_msg, R.drawable.ease_phone_pick,
    R.drawable.ease_video_camera,R.drawable.ease_search_msg,
)

val defaultGroupItemColor = intArrayOf(
    R.color.ease_group_detail_custom_layout_item_title_color,R.color.ease_group_detail_custom_layout_item_title_color,
    R.color.ease_group_detail_custom_layout_item_title_color,R.color.ease_group_detail_custom_layout_item_title_color,
)

val defaultGroupItemVisible = booleanArrayOf(
    true,false,false,false
)

val defaultGroupItemOrder = intArrayOf(
    2,1,3,4
)

val defaultContactItemIds = intArrayOf(
    R.id.extend_item_message,
    R.id.extend_item_audio_call,
    R.id.extend_item_video_call,
)

val defaultContactItemString = intArrayOf(
    R.string.ease_detail_item_message, R.string.ease_detail_item_audio,
    R.string.ease_detail_item_video
)

val defaultContactItemDrawables = intArrayOf(
    R.drawable.ease_bubble_msg, R.drawable.ease_phone_pick,
    R.drawable.ease_video_camera
)

val defaultContactItemColor = intArrayOf(
    R.color.ease_group_detail_custom_layout_item_title_color,R.color.ease_group_detail_custom_layout_item_title_color,
    R.color.ease_group_detail_custom_layout_item_title_color
)

val defaultContactItemVisible = booleanArrayOf(
    true,false,false
)

val defaultContactItemOrder = intArrayOf(
    1,0,2
)

