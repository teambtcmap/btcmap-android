package view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import map.getOnPrimaryContainerColor
import map.getPrimaryContainerColor
import org.btcmap.R

class IconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fillPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = fillColor
            isAntiAlias = true
        }
    }

    private val strokePaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = this@IconButton.strokeWidth
            color = strokeColor
            isAntiAlias = true
        }
    }

    var icon: Bitmap? = null
        private set

    var iconColor: Int = context.getOnPrimaryContainerColor()
        set(value) {
            field = value
            updateIcon()
        }

    var fillColor: Int = context.getPrimaryContainerColor()
        set(value) {
            field = value
            fillPaint.color = value
            invalidate()
        }

    var strokeColor: Int = context.getOnPrimaryContainerColor()
        set(value) {
            field = value
            strokePaint.color = value
            invalidate()
        }

    var strokeWidth: Float = resources.displayMetrics.density * 2
        set(value) {
            field = value
            strokePaint.strokeWidth = value
            invalidate()
        }

    private var iconResId: Int = R.drawable.icon_store

    init {
        // Read attributes from XML
        attrs?.let { parseAttributes(it) }
        updateIcon()
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.IconButton,
            0,
            0
        )

        try {
            // Get icon resource
            iconResId = typedArray.getResourceId(
                R.styleable.IconButton_iconSrc,
                R.drawable.icon_store
            )

            // Get colors
            iconColor = typedArray.getColor(
                R.styleable.IconButton_iconColor,
                context.getOnPrimaryContainerColor()
            )

            fillColor = typedArray.getColor(
                R.styleable.IconButton_fillColor,
                context.getPrimaryContainerColor()
            )

            strokeColor = typedArray.getColor(
                R.styleable.IconButton_strokeColor,
                context.getOnPrimaryContainerColor()
            )

            // Get stroke width
            strokeWidth = typedArray.getDimension(
                R.styleable.IconButton_strokeWidth,
                resources.displayMetrics.density * 2
            )

            if (typedArray.getBoolean(R.styleable.IconButton_selected, false)) {
                isSelected = true
            }
        } finally {
            typedArray.recycle()
        }
    }

    fun setDrawable(resId: Int) {
        iconResId = resId
        updateIcon()
    }

    private fun updateIcon() {
        val drawable = ContextCompat.getDrawable(context, iconResId) ?: return
        DrawableCompat.setTint(drawable, iconColor)
        val drawableSize = (resources.displayMetrics.density * 22).toInt()
        icon = drawable.toBitmap(width = drawableSize, height = drawableSize)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawCircle(centerX, centerY, width / 2.0f, fillPaint)

        if (isSelected) {
            canvas.drawCircle(
                centerX,
                centerY,
                width / 2.0f - strokePaint.strokeWidth / 2,
                strokePaint
            )
        }

        val icon = this.icon
        if (icon != null) {
            canvas.drawBitmap(
                icon,
                width / 2f - icon.width / 2f,
                height / 2f - icon.height / 2f,
                null
            )
        }
    }
}