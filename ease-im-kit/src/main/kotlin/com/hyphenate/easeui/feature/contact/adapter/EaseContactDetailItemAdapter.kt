package com.hyphenate.easeui.feature.contact.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.hyphenate.easeui.R
import com.hyphenate.easeui.interfaces.OnMenuItemClickListener
import com.hyphenate.easeui.model.EaseMenuItem
import com.hyphenate.easeui.widget.EaseImageView


class EaseContactDetailItemAdapter(
    context: Context,
    resource: Int,
    private val objects: MutableList<EaseMenuItem>
) : ArrayAdapter<EaseMenuItem>(context, resource, objects){
    private var listener: OnMenuItemClickListener? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.layout_contact_detail_item, null)
        }
        val title = convertView!!.findViewById<TextView>(R.id.itemTitle)
        val icon = convertView.findViewById<EaseImageView>(R.id.itemIcon)
        val item: EaseMenuItem? = getItem(position)
        item?.let {
            title.text = it.title
            title.setTextColor(it.titleColor)
            icon.setBackgroundResource(it.resourceId)
            if (it.isVisible){
                convertView.visibility = View.VISIBLE
            }else{
                convertView.visibility = View.GONE
            }
        }
        convertView.setOnClickListener{
            listener?.onMenuItemClick(item,position)
        }
        return convertView
    }

    override fun getCount(): Int {
        return super.getCount()
    }

    fun setContactDetailItemClickListener(listener: OnMenuItemClickListener){
        this.listener = listener
    }
}