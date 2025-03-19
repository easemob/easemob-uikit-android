package com.hyphenate.easeui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.hyphenate.easeui.R
import java.lang.reflect.Method

/**
 * Canvas#save(int) has been removed from sdk-28, see detail from:
 * https://issuetracker.google.com/issues/110856542
 * so this helper classes uses reflection to access the API on older devices.
 */
internal object CanvasLegacy {
    var MATRIX_SAVE_FLAG = 0
    var CLIP_SAVE_FLAG = 0
    var HAS_ALPHA_LAYER_SAVE_FLAG = 0
    var FULL_COLOR_LAYER_SAVE_FLAG = 0
    var CLIP_TO_LAYER_SAVE_FLAG = 0
    private var SAVE: Method? = null

    init {
        try {
            MATRIX_SAVE_FLAG = Canvas::class.java.getField("MATRIX_SAVE_FLAG")[null] as Int
            CLIP_SAVE_FLAG = Canvas::class.java.getField("CLIP_SAVE_FLAG")[null] as Int
            HAS_ALPHA_LAYER_SAVE_FLAG =
                Canvas::class.java.getField("HAS_ALPHA_LAYER_SAVE_FLAG")[null] as Int
            FULL_COLOR_LAYER_SAVE_FLAG =
                Canvas::class.java.getField("FULL_COLOR_LAYER_SAVE_FLAG")[null] as Int
            CLIP_TO_LAYER_SAVE_FLAG =
                Canvas::class.java.getField("CLIP_TO_LAYER_SAVE_FLAG")[null] as Int
            SAVE = Canvas::class.java.getMethod(
                "saveLayer",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Paint::class.java,
                Int::class.javaPrimitiveType
            )
        } catch (e: Throwable) {
            throw sneakyThrow(e)
        }
    }

    fun saveLayer(
        canvas: Canvas?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint?,
        saveFlags: Int
    ) {
        try {
            SAVE!!.invoke(canvas, left, top, right, bottom, paint, saveFlags)
        } catch (e: Throwable) {
            throw sneakyThrow(e)
        }
    }

    private fun sneakyThrow(t: Throwable?): RuntimeException {
        if (t == null) throw NullPointerException("t")
        return sneakyThrow0(t)
    }

    private fun <T : Throwable> sneakyThrow0(t: Throwable): T {
        throw t as T
    }
}

/**
 * Customized ImageView，Rounded Rectangle and border is implemented, and change color when you press
 */
class ChatUIKitImageView : AppCompatImageView {
    // paint when user press
    private var pressPaint: Paint? = null
    private var width = 0
    private var height = 0

    // border color
    private var borderColor = 0

    // width of border
    private var borderWidth = 0

    // alpha when pressed
    private var pressAlpha = 0

    // color when pressed
    private var pressColor = 0

    // radius
    private var radius = 0

    // rectangle or round, 1 is circle, 2 is rectangle
    private var shapeType = 0
    
    // Add a new paint for drawing border
    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    
    // Add a rounded rectangle path for clipping
    private val roundRectPath = android.graphics.Path()
    private val roundRectRectF = RectF()
    
    // Flag to track if GIF is being displayed
    private var isDisplayingGif = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        //init the value
        borderWidth = 0
        borderColor = -0x22000001
        pressAlpha = 0x42
        pressColor = 0x42000000
        radius = 16
        shapeType = 0

        // get attribute of ChatUIKitImageView
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.ChatUIKitImageView)
            borderColor = array.getColor(R.styleable.ChatUIKitImageView_ease_border_color, borderColor)
            borderWidth = array.getDimensionPixelOffset(
                R.styleable.ChatUIKitImageView_ease_border_width,
                borderWidth
            )
            pressAlpha = array.getInteger(R.styleable.ChatUIKitImageView_ease_press_alpha, pressAlpha)
            pressColor = array.getColor(R.styleable.ChatUIKitImageView_ease_press_color, pressColor)
            radius = array.getDimensionPixelOffset(R.styleable.ChatUIKitImageView_ease_radius, radius)
            shapeType = array.getInteger(R.styleable.ChatUIKitImageView_image_shape_type, shapeType)
            array.recycle()
        }

        // set paint when pressed
        pressPaint = Paint()
        pressPaint!!.isAntiAlias = true
        pressPaint!!.style = Paint.Style.FILL
        pressPaint!!.color = pressColor
        pressPaint!!.alpha = 0
        pressPaint!!.flags = Paint.ANTI_ALIAS_FLAG
        
        // Configure border paint
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth.toFloat()
        
        // We need hardware acceleration for better GIF rendering
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    // Override setImageDrawable to detect GIFs
    override fun setImageDrawable(drawable: Drawable?) {
        isDisplayingGif = drawable != null && 
                drawable.javaClass.name.contains("Gif", ignoreCase = true)
        super.setImageDrawable(drawable)
    }

    override fun onDraw(canvas: Canvas) {
        if (shapeType == 0 || drawable == null) {
            super.onDraw(canvas)
            return
        }
        
        // Save the canvas state
        canvas.save()
        
        // Apply the shape mask
        applyShapeMask(canvas)
        
        // Draw the image content
        super.onDraw(canvas)
        
        // Restore to draw border and press effects outside the clipping
        canvas.restore()
        
        if (isClickable) {
            drawPress(canvas)
        }
        
        drawBorder(canvas)
    }
    
    /**
     * Apply the shape mask to the canvas based on shapeType
     */
    private fun applyShapeMask(canvas: Canvas) {
        if (shapeType == 1) {
            // Circle shape
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = Math.min(width, height) / 2f - borderWidth / 2f
            
            // Create a circular clip path
            val path = android.graphics.Path()
            path.addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
            canvas.clipPath(path)
        } else if (shapeType == 2) {
            // Rounded rectangle
            roundRectRectF.set(
                borderWidth / 2f,
                borderWidth / 2f,
                width - borderWidth / 2f,
                height - borderWidth / 2f
            )
            
            // Create a rounded rectangle clip path
            roundRectPath.reset()
            roundRectPath.addRoundRect(
                roundRectRectF,
                radius.toFloat(),
                radius.toFloat(),
                android.graphics.Path.Direction.CW
            )
            canvas.clipPath(roundRectPath)
        }
    }

    /**
     * draw the effect when pressed
     */
    private fun drawPress(canvas: Canvas) {
        pressPaint?.let { paint ->
            if (shapeType == 1) {
                canvas.drawCircle(
                    width / 2f,
                    height / 2f,
                    Math.min(width, height) / 2f - borderWidth / 2f,
                    paint
                )
            } else if (shapeType == 2) {
                val rectF = RectF(
                    borderWidth / 2f,
                    borderWidth / 2f,
                    width - borderWidth / 2f,
                    height - borderWidth / 2f
                )
                canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)
            }
        }
    }

    /**
     * draw customized border
     */
    private fun drawBorder(canvas: Canvas) {
        if (borderWidth > 0) {
            borderPaint.color = borderColor
            
            if (shapeType == 1) {
                // Circle border
                val centerX = width / 2f
                val centerY = height / 2f
                val radius = Math.min(width, height) / 2f - borderWidth / 2f
                canvas.drawCircle(centerX, centerY, radius, borderPaint)
            } else if (shapeType == 2) {
                // Rounded rectangle border
                val rectF = RectF(
                    borderWidth / 2f,
                    borderWidth / 2f,
                    width - borderWidth / 2f,
                    height - borderWidth / 2f
                )
                canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), borderPaint)
            }
        }
    }

    /**
     * monitor the size change
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h
    }

    /**
     * monitor if touched
     *
     * @param event
     * @return
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressPaint!!.alpha = pressAlpha
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                pressPaint!!.alpha = 0
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {}
            else -> {
                pressPaint!!.alpha = 0
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * set border color
     */
    fun setBorderColor(borderColor: Int) {
        this.borderColor = borderColor
        invalidate()
    }

    /**
     * set border width
     */
    fun setBorderWidth(borderWidth: Int) {
        this.borderWidth = borderWidth
        invalidate()
    }

    /**
     * set alpha when pressed
     *
     * @param pressAlpha
     */
    fun setPressAlpha(pressAlpha: Int) {
        this.pressAlpha = pressAlpha
    }

    /**
     * set color when pressed
     *
     * @param pressColor
     */
    fun setPressColor(pressColor: Int) {
        this.pressColor = pressColor
    }

    /**
     * set radius
     *
     * @param radius
     */
    fun setRadius(radius: Int) {
        this.radius = radius
        invalidate()
    }

    /**
     * set shape,1 is circle, 2 is rectangle
     *
     * @param shapeType
     */
    fun setShapeType(shapeType: Int) {
        this.shapeType = shapeType
        invalidate()
    }

    /**
     * set shape type
     * @param shapeType
     */
    fun setShapeType(shapeType: ShapeType?) {
        if (shapeType == null) {
            return
        }
        this.shapeType = shapeType.ordinal
        invalidate()
    }

    /**
     * 图片形状
     */
    enum class ShapeType {
        NONE, ROUND, RECTANGLE
    }

    companion object {
        // default bitmap config
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
        private const val COLORDRAWABLE_DIMENSION = 1
    }
}