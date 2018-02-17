/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.repository.placeicon

import android.content.Context
import android.graphics.*
import com.bubelov.coins.R

import javax.inject.Inject
import javax.inject.Singleton
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.DrawableRes
import android.support.graphics.drawable.VectorDrawableCompat
import android.graphics.drawable.VectorDrawable
import android.support.v4.content.ContextCompat
import androidx.graphics.applyCanvas
import androidx.graphics.createBitmap
import androidx.graphics.drawable.toBitmap
import androidx.graphics.toColorFilter
import androidx.graphics.toRect

@Singleton
class PlaceIconsRepository @Inject constructor(
    private val context: Context
) {
    private val markersCache = mutableMapOf<String, Bitmap>()

    private val emptyPinBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.ic_map_marker_empty
    )

    private val pinCirclePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val pinIconPaint = Paint().apply {
        colorFilter = PorterDuff.Mode.SRC_IN.toColorFilter(
            ContextCompat.getColor(context, R.color.primary_dark)
        )

        isAntiAlias = true
    }

    fun getMarker(placeCategory: String): Bitmap {
        var marker = markersCache[placeCategory]

        if (marker == null) {
            marker = createMarker(placeCategory)
            markersCache[placeCategory] = marker
        }

        return marker
    }

    fun getPlaceIcon(category: String): Bitmap {
        val iconId = getIconResId(category) ?: R.drawable.ic_place
        return ContextCompat.getDrawable(context, iconId)!!.toBitmap()
    }

    private fun getIconResId(category: String): Int? {
        return when (category.toLowerCase()) {
            "atm" -> R.drawable.ic_atm
            "restaurant" -> R.drawable.ic_restaurant
            "café" -> R.drawable.ic_cafe
            "bar" -> R.drawable.ic_bar
            "hotel" -> R.drawable.ic_hotel
            "pizza" -> R.drawable.ic_pizza
            "fast food" -> R.drawable.ic_fast_food
            "hospital" -> R.drawable.ic_hospital
            "pharmacy" -> R.drawable.ic_pharmacy
            "taxi" -> R.drawable.ic_taxi
            "gas station" -> R.drawable.ic_gas_station
            else -> null
        }
    }

    private fun createMarker(placeCategory: String): Bitmap {
        val pinBitmap = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        val iconResId = getIconResId(placeCategory) ?: return pinBitmap

        pinBitmap.applyCanvas {
            drawCircle(
                pinBitmap.width.toFloat() / 2,
                pinBitmap.height.toFloat() * 0.43f,
                pinBitmap.width.toFloat() * 0.27f,
                pinCirclePaint
            )
        }

        val iconFrame = RectF(
            pinBitmap.width.toFloat() * 0.3f,
            pinBitmap.width.toFloat() * 0.23f,
            pinBitmap.width.toFloat() * 0.7f,
            pinBitmap.height.toFloat() * 0.63f
        ).toRect()

        val iconBitmap = toBitmap(
            iconResId,
            iconFrame.right - iconFrame.left,
            iconFrame.bottom - iconFrame.top
        )

        pinBitmap.applyCanvas {
            drawBitmap(iconBitmap, null, iconFrame, pinIconPaint)
        }

        return pinBitmap
    }

    private fun toBitmap(
        @DrawableRes drawableId: Int,
        preferredWidth: Int,
        preferredHeight: Int
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)

        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else if (drawable is VectorDrawable || drawable is VectorDrawableCompat) {
            val bitmap = createBitmap(preferredWidth, preferredHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } else {
            throw IllegalArgumentException("Unsupported drawable")
        }
    }
}