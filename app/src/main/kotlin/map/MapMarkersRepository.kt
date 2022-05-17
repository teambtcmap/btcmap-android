package map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorFilter
import androidx.core.graphics.toRect
import db.Place
import icons.iconResId
import org.btcmap.R
import org.koin.core.annotation.Single

@Single
class MapMarkersRepository(
    private val context: Context,
) {

    private val cache = mutableMapOf<Int?, BitmapDrawable>()

    fun getMarker(place: Place): BitmapDrawable {
        val iconResId = place.iconResId()
        var markerDrawable = cache[iconResId]

        if (markerDrawable == null) {
            markerDrawable = createMarkerIcon(iconResId).toDrawable(context.resources)
            cache[iconResId] = markerDrawable
        }

        return markerDrawable
    }

    private fun createMarkerIcon(iconResId: Int?): Bitmap {
        val emptyPinBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_map_marker_empty,
        )

        val markerIcon = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        if (iconResId != null) {
            markerIcon.applyCanvas {
                drawCircle(
                    markerIcon.width.toFloat() / 2,
                    markerIcon.height.toFloat() * 0.43f,
                    markerIcon.width.toFloat() * 0.27f,
                    Paint().apply {
                        color = Color.WHITE
                        isAntiAlias = true
                    },
                )
            }

            val iconFrame = RectF(
                markerIcon.width.toFloat() * 0.3f,
                markerIcon.width.toFloat() * 0.23f,
                markerIcon.width.toFloat() * 0.7f,
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
                        colorFilter = PorterDuff.Mode.SRC_IN.toColorFilter(Color.parseColor("#ff9100"))
                        isAntiAlias = true
                    },
                )
            }
        }

        return markerIcon
    }
}