package com.hyphenate.easeui.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.hyphenate.easeui.R

class EaseArrowItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ConstraintLayout(context, attrs, defStyleAttr) {
    var avatar: EaseImageView? = null
        private set
    var tvTitle: TextView? = null
        private set
    var tvContent: TextView? = null
        private set
    var rightTitle: TextView? = null
        private set
    private var ivArrow: ImageView? = null
    private var viewDivider: View? = null
    private var title: String? = null
    private var content: String? = null
    private var titleColor = 0
    private var contentColor = 0
    private var titleSize = 0f
    private var contentSize = 0f
    private var root: View? = null

    init {
        init(context, attrs)
    }

    fun init(context: Context, attrs: AttributeSet?) {
        root = LayoutInflater.from(context).inflate(R.layout.ease_layout_item_arrow, this)
        avatar = findViewById(R.id.avatar)
        tvTitle = findViewById(R.id.tv_title)
        tvContent = findViewById(R.id.tv_content)
        ivArrow = findViewById(R.id.iv_arrow)
        viewDivider = findViewById(R.id.view_divider)
        rightTitle = findViewById(R.id.tv_right)
        val a = context.obtainStyledAttributes(attrs, R.styleable.EaseArrowItemView)
        val titleResourceId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemTitle, -1)
        title = a.getString(R.styleable.EaseArrowItemView_arrowItemTitle)
        if (titleResourceId != -1) {
            title = getContext().getString(titleResourceId)
        }
        tvTitle?.text = title
        val titleColorId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemTitleColor, -1)
        titleColor = a.getColor(
            R.styleable.EaseArrowItemView_arrowItemTitleColor,
            ContextCompat.getColor(getContext(), R.color.ease_color_on_background_low)
        )
        if (titleColorId != -1) {
            titleColor = ContextCompat.getColor(getContext(), titleColorId)
        }
        tvTitle?.setTextColor(titleColor)
        val titleStyle = a.getInteger(R.styleable.EaseArrowItemView_arrowItemTitleStyle, -1)
        setTvStyle(titleStyle)
        val titleSizeId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemTitleSize, -1)
        titleSize =
            a.getDimension(R.styleable.EaseArrowItemView_arrowItemTitleSize, sp2px(getContext(), 14f))
        if (titleSizeId != -1) {
            titleSize = resources.getDimension(titleSizeId)
        }
        tvTitle?.paint?.textSize = titleSize
        val contentResourceId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemContent, -1)
        content = a.getString(R.styleable.EaseArrowItemView_arrowItemContent)
        if (contentResourceId != -1) {
            content = getContext().getString(contentResourceId)
        }
        tvContent?.text = content
        val contentColorId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemContentColor, -1)
        contentColor = a.getColor(
            R.styleable.EaseArrowItemView_arrowItemContentColor,
            ContextCompat.getColor(getContext(), R.color.ease_color_on_background_low)
        )
        if (contentColorId != -1) {
            contentColor = ContextCompat.getColor(getContext(), contentColorId)
        }
        tvContent?.setTextColor(contentColor)
        val contentSizeId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemContentSize, -1)
        contentSize =
            a.getDimension(R.styleable.EaseArrowItemView_arrowItemContentSize, sp2px(getContext(), 14f))
        if (contentSizeId != -1) {
            contentSize = resources.getDimension(contentSizeId)
        }
        tvContent?.paint?.textSize = contentSize
        val showDivider = a.getBoolean(R.styleable.EaseArrowItemView_arrowItemShowDivider, true)
        viewDivider?.visibility = if (showDivider) VISIBLE else GONE
        val showArrow = a.getBoolean(R.styleable.EaseArrowItemView_arrowItemShowArrow, true)
        ivArrow?.visibility = if (showArrow) VISIBLE else GONE
        val showAvatar = a.getBoolean(R.styleable.EaseArrowItemView_arrowItemShowAvatar, false)
        avatar?.visibility = if (showAvatar) VISIBLE else GONE
        val arrowSrcResourceId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemArrowSrc, -1)
        if (arrowSrcResourceId != -1) {
            ivArrow?.setImageResource(arrowSrcResourceId)
        }
        val avatarSrcResourceId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemAvatarSrc, -1)
        if (avatarSrcResourceId != -1) {
            avatar?.setImageResource(avatarSrcResourceId)
        }
        val avatarHeightId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemAvatarHeight, -1)
        var height = a.getDimension(R.styleable.EaseArrowItemView_arrowItemAvatarHeight, 0f)
        if (avatarHeightId != -1) {
            height = resources.getDimension(avatarHeightId)
        }
        val avatarWidthId = a.getResourceId(R.styleable.EaseArrowItemView_arrowItemAvatarWidth, -1)
        var width = a.getDimension(R.styleable.EaseArrowItemView_arrowItemAvatarWidth, 0f)
        if (avatarWidthId != -1) {
            width = resources.getDimension(avatarWidthId)
        }
        a.recycle()
        val params = avatar?.layoutParams
        params?.height = if (height == 0f) ViewGroup.LayoutParams.WRAP_CONTENT else height.toInt()
        params?.width = if (width == 0f) ViewGroup.LayoutParams.WRAP_CONTENT else width.toInt()
    }

    fun getTitle(): String {
        return tvTitle?.text.toString().trim { it <= ' ' }
    }

    fun setTitle(title: String?) {
        tvTitle?.text = title
    }

    fun setContent(content: String?) {
        tvContent?.text = content
    }

    fun setArrow(resourceId: Int) {
        ivArrow?.setImageResource(resourceId)
    }

    fun setArrowVisibility(visibility: Int) {
        ivArrow?.visibility = visibility
    }

    fun setItemDividerVisibility(visibility: Int){
        viewDivider?.visibility = visibility
    }

    fun setAvatar(resourceId: Int) {
        avatar?.setImageResource(resourceId)
    }

    fun setAvatarVisibility(visibility: Int) {
        avatar?.visibility = visibility
    }

    fun setAvatarMargin(left: Int, top: Int, right: Int, bottom: Int) {
        val params = avatar?.layoutParams as LayoutParams
        params.setMargins(left, top, right, bottom)
    }

    fun setAvatarHeight(height: Int) {
        val params = avatar?.layoutParams
        params?.height = height
        avatar?.layoutParams = params
    }

    fun setAvatarWidth(width: Int) {
        val params = avatar?.layoutParams
        params?.width = width
        avatar?.layoutParams = params
    }

     fun setTvStyle(titleStyle: Int) {
        when (titleStyle) {
            0 -> tvTitle?.setTypeface(null, Typeface.NORMAL)
            1 -> tvTitle?.setTypeface(null, Typeface.BOLD)
            2 -> tvTitle?.setTypeface(null, Typeface.ITALIC)
        }
    }

    fun setTitleColor(titleColor: Int) {
        tvTitle?.setTextColor(titleColor)
    }

    fun setContentColor(contentColor: Int) {
        tvContent?.setTextColor(contentColor)
    }

    fun setTitleSize(titleSize: Float) {
        tvTitle?.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
    }

    fun setContentSize(contentSize: Float) {
        tvContent?.textSize = contentSize
    }

    companion object {
        /**
         * sp to px
         *
         * @param context
         * @param value
         * @return
         */
        fun sp2px(context: Context, value: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                context.resources.displayMetrics
            )
        }
    }
}