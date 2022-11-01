package icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.applyCanvas
import map.getOnPrimaryContainerColor

fun Context.createFontIconBitmap(iconId: String, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

    val paint = Paint().apply {
        typeface = iconTypeface()
        textSize = sizePx.toFloat()
        color = getOnPrimaryContainerColor()
        isAntiAlias = true
    }

    bitmap.applyCanvas {
        drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), Paint().apply { color = Color.RED })
        val textWidth = paint.measureText(iconId)
        drawText(iconId, width.toFloat() / 2f - textWidth / 2f, height.toFloat() / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f, paint)
    }

    return bitmap
}

private var iconTypeface: Typeface? = null

fun Context.iconTypeface(): Typeface {
    if (iconTypeface == null) {
        iconTypeface = Typeface.Builder(assets, "icons.ttf")
            .setFontVariationSettings("'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24")
            .build()
    }

    return iconTypeface!!
}