package map

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import icons.iconTypeface
import org.btcmap.R

private const val PIN_SIZE_DP = 48f

fun Context.marker(
    iconId: String,
    backgroundColor: Int,
    iconColor: Int,
    counter: Long
): BitmapDrawable {
    val pinSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
    ).toInt()

    val emptyPinDrawable = ContextCompat.getDrawable(this, R.drawable.marker)!!

    DrawableCompat.setTint(
        emptyPinDrawable,
        backgroundColor,
    )

    val emptyPinBitmap = emptyPinDrawable.toBitmap(width = pinSizePx, height = pinSizePx)

    val markerIcon = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
        drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
    }

    val iconPaint = Paint().apply {
        val pinSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            PIN_SIZE_DP,
            resources.displayMetrics,
        )

        typeface = iconTypeface()
        textSize = pinSizePx / 2.1f
        isAntiAlias = true
    }

    if (iconId.isNotBlank()) {
        markerIcon.applyCanvas {
            val textWidth = iconPaint.measureText(iconId)
            iconPaint.color = iconColor
            drawText(
                iconId,
                markerIcon.width / 2f - textWidth / 2f,
                markerIcon.height / 2f - (iconPaint.fontMetrics.ascent + iconPaint.fontMetrics.descent) / 2 - markerIcon.height.toFloat() * 0.09f,
                iconPaint
            )
        }
    }

    if (counter > 0) {
        val commentCountCirclePaint by lazy {
            Paint().apply {
                color = Color.parseColor("#0c9073")
                isAntiAlias = true
            }
        }

        val commentsCountTextPaint by lazy {
            Paint().apply {
                val pinSizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    PIN_SIZE_DP,
                    resources.displayMetrics,
                )
                textSize = pinSizePx / 4f
                color = Color.WHITE
                isAntiAlias = true
            }
        }

        markerIcon.applyCanvas {
            val textWidth = commentsCountTextPaint.measureText(counter.toString())
            drawOval(
                markerIcon.width.toFloat() * 0.65f,
                0f,
                markerIcon.width.toFloat() * 0.65f + markerIcon.width.toFloat() * 0.35f,
                markerIcon.height.toFloat() * 0.35f,
                commentCountCirclePaint,
            )
            drawText(
                counter.toString(),
                markerIcon.width * 0.82f - textWidth / 2f,
                markerIcon.height / 4f - (commentsCountTextPaint.fontMetrics.ascent + commentsCountTextPaint.fontMetrics.descent) / 2 - markerIcon.height.toFloat() * 0.07f,
                commentsCountTextPaint,
            )
        }
    }

    return markerIcon.toDrawable(resources)
}