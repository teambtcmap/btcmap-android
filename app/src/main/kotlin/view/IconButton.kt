package view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
            color = context.getPrimaryContainerColor()
            isAntiAlias = true
        }
    }

    private val strokePaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 2
            color = context.getOnPrimaryContainerColor()
            isAntiAlias = true
        }
    }

    var icon: Bitmap? = null

    var iconColor: Int = Color.GREEN

    init {
        iconColor = context.getOnPrimaryContainerColor()
        setDrawable(R.drawable.store)
    }

    fun setDrawable(resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId)!!
        DrawableCompat.setTint(
            drawable,
            iconColor,
        )
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