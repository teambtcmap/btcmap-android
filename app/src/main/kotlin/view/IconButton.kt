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
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var iconResId = R.drawable.icon_store

    var iconColor = context.getOnPrimaryContainerColor()

    var icon: Bitmap? = null
        private set

    init {
        attrs?.let { parseAttributes(it) }
    }

    fun backgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun borderColor(color: Int) {
        borderPaint.color = color
        invalidate()
    }

    fun icon(resId: Int, color: Int) {
        icon = generateIcon(resId, color)
        invalidate()
    }

    fun iconResId(id: Int) {
        iconResId = id
        icon(iconResId, iconColor)
    }

    fun iconColor(color: Int) {
        iconColor = color
        icon(iconResId, iconColor)
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.IconButton,
            0,
            0
        )

        try {
            backgroundColor(
                typedArray.getColor(
                    R.styleable.IconButton_backgroundColor,
                    context.getPrimaryContainerColor(),
                )
            )

            borderColor(
                typedArray.getColor(
                    R.styleable.IconButton_borderColor,
                    context.getOnPrimaryContainerColor(),
                )
            )

            borderPaint.strokeWidth = typedArray.getDimension(
                R.styleable.IconButton_borderWidth,
                resources.displayMetrics.density * 2,
            )

            iconResId = typedArray.getResourceId(
                R.styleable.IconButton_iconSrc,
                R.drawable.icon_store,
            )

            iconColor = typedArray.getColor(
                R.styleable.IconButton_iconColor,
                context.getOnPrimaryContainerColor(),
            )

            icon(iconResId, iconColor)

            if (typedArray.getBoolean(R.styleable.IconButton_selected, false)) {
                isSelected = true
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun generateIcon(iconResId: Int, color: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, iconResId) ?: return null
        DrawableCompat.setTint(drawable, color)
        val drawableSize = (resources.displayMetrics.density * 22).toInt()
        return drawable.toBitmap(width = drawableSize, height = drawableSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawCircle(centerX, centerY, width / 2.0f, backgroundPaint)

        if (isSelected) {
            canvas.drawCircle(
                centerX,
                centerY,
                width / 2.0f - borderPaint.strokeWidth / 2,
                borderPaint,
            )
        }

        icon?.let { icon ->
            canvas.drawBitmap(
                icon,
                width / 2f - icon.width / 2f,
                height / 2f - icon.height / 2f,
                null,
            )
        }
    }
}