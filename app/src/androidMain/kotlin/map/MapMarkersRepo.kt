package map

import android.content.Context
import android.graphics.Bitmap
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

class MapMarkersRepo(
    private val context: Context,
) {

    private val iconPaint by lazy {
        Paint().apply {
            val pinSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PIN_SIZE_DP,
                context.resources.displayMetrics,
            )

            typeface = context.iconTypeface()
            textSize = pinSizePx / 2.1f
            color = context.getOnPrimaryContainerColor()
            isAntiAlias = true
        }
    }

    private val whiteIconPaint by lazy {
        Paint().apply {
            val pinSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PIN_SIZE_DP,
                context.resources.displayMetrics,
            )

            typeface = context.iconTypeface()
            textSize = pinSizePx / 2.1f
            color = Color.WHITE
            isAntiAlias = true
        }
    }

    private val commentCountCirclePaint by lazy {
        Paint().apply {
            color = Color.parseColor("#0c9073")
            isAntiAlias = true
        }
    }

    private val commentsCountTextPaint by lazy {
        Paint().apply {
            val pinSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PIN_SIZE_DP,
                context.resources.displayMetrics,
            )
            textSize = pinSizePx / 4f
            color = Color.WHITE
            isAntiAlias = true
        }
    }

    private val cache = mutableMapOf<String?, BitmapDrawable>()

    private val boostedCache = mutableMapOf<String?, BitmapDrawable>()

    private val warningCache = mutableMapOf<String?, BitmapDrawable>()

    val meetupMarker by lazy {
        createMarkerIcon("groups", BackgroundType.MEETUP, 0).toDrawable(context.resources)
    }

    fun getMarker(iconId: String, comments: Long): BitmapDrawable {
        var markerDrawable = if (comments == 0L) null else cache[iconId]

        if (markerDrawable == null) {
            markerDrawable =
                createMarkerIcon(
                    iconId,
                    BackgroundType.PRIMARY_CONTAINER,
                    comments,
                ).toDrawable(context.resources)
            if (comments == 0L) {
                cache[iconId] = markerDrawable
            }
        }

        return markerDrawable
    }

    fun getBoostedMarker(iconId: String, comments: Long): BitmapDrawable {
        var markerDrawable = if (comments == 0L) null else boostedCache[iconId]

        if (markerDrawable == null) {
            markerDrawable =
                createMarkerIcon(
                    iconId,
                    BackgroundType.BOOSTED,
                    comments
                ).toDrawable(context.resources)
            if (comments == 0L) {
                boostedCache[iconId] = markerDrawable
            }
        }

        return markerDrawable
    }

    fun getWarningMarker(iconId: String): BitmapDrawable {
        var markerDrawable = warningCache[iconId]

        if (markerDrawable == null) {
            markerDrawable =
                createMarkerIcon(iconId, BackgroundType.WARNING, 0).toDrawable(context.resources)
            warningCache[iconId] = markerDrawable
        }

        return markerDrawable
    }

    private fun createMarkerIcon(
        iconId: String,
        backgroundType: BackgroundType,
        comments: Long
    ): Bitmap {
        val pinSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 48f, context.resources.displayMetrics
        ).toInt()

        val emptyPinDrawable = ContextCompat.getDrawable(context, R.drawable.marker)!!

        when (backgroundType) {
            BackgroundType.PRIMARY_CONTAINER -> DrawableCompat.setTint(
                emptyPinDrawable,
                context.getPrimaryContainerColor()
            )

            BackgroundType.BOOSTED -> DrawableCompat.setTint(
                emptyPinDrawable,
                Color.parseColor("#f7931a")
            )

            BackgroundType.MEETUP -> DrawableCompat.setTint(
                emptyPinDrawable,
                Color.parseColor("#0e95af")
            )

            BackgroundType.WARNING -> DrawableCompat.setTint(
                emptyPinDrawable,
                context.getPrimaryContainerColor(),
            )
        }

        val emptyPinBitmap = emptyPinDrawable.toBitmap(width = pinSizePx, height = pinSizePx)

        val markerIcon = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        if (iconId.isNotBlank()) {
            markerIcon.applyCanvas {
                val textWidth = iconPaint.measureText(iconId)

//                drawBitmap(
//                    context.createFontIconBitmap(pinSizePx  / 2),
//                    (markerIcon.width / 2 - pinSizePx / 2 / 2).toFloat(),
//                    (markerIcon.height / 2 - pinSizePx / 2 / 2).toFloat() - markerIcon.height.toFloat() * 0.09f,
//                    Paint()
//                )

                if (backgroundType == BackgroundType.WARNING) {
                    iconPaint.color = context.getErrorColor()
                } else {
                    iconPaint.color = context.getOnPrimaryContainerColor()
                }

                drawText(
                    iconId,
                    markerIcon.width / 2f - textWidth / 2f,
                    markerIcon.height / 2f - (iconPaint.fontMetrics.ascent + iconPaint.fontMetrics.descent) / 2 - markerIcon.height.toFloat() * 0.09f,
                    if (backgroundType == BackgroundType.BOOSTED || backgroundType == BackgroundType.MEETUP) whiteIconPaint else iconPaint
                )
            }
        }

        if (comments > 0) {
            markerIcon.applyCanvas {
                val textWidth = commentsCountTextPaint.measureText(comments.toString())
                drawOval(
                    markerIcon.width.toFloat() * 0.65f,
                    0f,
                    markerIcon.width.toFloat() * 0.65f + markerIcon.width.toFloat() * 0.35f,
                    markerIcon.height.toFloat() * 0.35f,
                    commentCountCirclePaint,
                )
                drawText(
                    comments.toString(),
                    markerIcon.width * 0.82f - textWidth / 2f,
                    markerIcon.height / 4f - (commentsCountTextPaint.fontMetrics.ascent + commentsCountTextPaint.fontMetrics.descent) / 2 - markerIcon.height.toFloat() * 0.07f,
                    commentsCountTextPaint,
                )
            }
        }

        return markerIcon
    }

    companion object {
        private const val PIN_SIZE_DP = 48f
    }

    enum class BackgroundType {
        PRIMARY_CONTAINER,
        BOOSTED,
        WARNING,
        MEETUP,
    }
}