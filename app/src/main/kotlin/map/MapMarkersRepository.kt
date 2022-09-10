package map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorFilter
import androidx.core.graphics.toRect
import db.Element
import icons.iconResId
import org.btcmap.R
import org.koin.core.annotation.Single

@Single
class MapMarkersRepository(
    private val context: Context,
) {

    private val cache = mutableMapOf<Int?, BitmapDrawable>()

    fun getMarker(element: Element): BitmapDrawable {
        val iconResId = element.iconResId()
        var markerDrawable = cache[iconResId]

        if (markerDrawable == null) {
            markerDrawable = createMarkerIcon(iconResId).toDrawable(context.resources)
            cache[iconResId] = markerDrawable
        }

        return markerDrawable
    }

    fun invalidateCache() {
        cache.clear()
    }

    private fun createMarkerIcon(iconResId: Int?): Bitmap {
        val pinSizePx =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.resources.displayMetrics).toInt()

        val emptyPinBitmap = ContextCompat.getDrawable(context, R.drawable.ic_marker)!!.toBitmap(
            width = pinSizePx,
            height = pinSizePx,
        )

        val markerIcon = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        if (iconResId != null) {
            markerIcon.applyCanvas {
                drawCircle(
                    markerIcon.width.toFloat() / 2,
                    markerIcon.height.toFloat() * 0.4f,
                    markerIcon.width.toFloat() * 0.325f,
                    Paint().apply {
                        color = ContextCompat.getColor(context, R.color.pin_icon_bg)
                        isAntiAlias = true
                    },
                )
            }

            val iconFrame = RectF(
                markerIcon.width.toFloat() * 0.27f,
                markerIcon.width.toFloat() * 0.17f,
                markerIcon.width.toFloat() * 0.73f,
                markerIcon.height.toFloat() * 0.63f
            ).toRect()

            val iconBitmap = ContextCompat.getDrawable(context, iconResId)!!.toBitmap(
                width = iconFrame.right - iconFrame.left,
                height = iconFrame.bottom - iconFrame.top,
            )

            markerIcon.applyCanvas {
                drawBitmap(
                    iconBitmap,
                    null,
                    iconFrame,
                    Paint().apply {
                        colorFilter =
                            PorterDuff.Mode.SRC_IN.toColorFilter(ContextCompat.getColor(context, R.color.pin_icon))
                        isAntiAlias = true
                    },
                )
            }
        }

        return markerIcon
    }
}