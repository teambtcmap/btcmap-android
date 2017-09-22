package com.bubelov.coins.repository.placemarker

import android.content.Context
import android.graphics.*
import com.bubelov.coins.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import javax.inject.Inject
import javax.inject.Singleton
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.DrawableRes
import android.support.graphics.drawable.VectorDrawableCompat
import android.graphics.drawable.VectorDrawable
import android.support.v4.content.ContextCompat

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceMarkersRepository @Inject
internal constructor(private val context: Context) {
    private val cache = mutableMapOf<String, BitmapDescriptor>()

    fun getPlaceCategoryMarker(category: String): BitmapDescriptor {
        var marker = cache[category]

        if (marker == null) {
            marker = createBitmapDescriptor(category)
            cache[category] = marker
        }

        return marker
    }

    private fun createBitmapDescriptor(placeCategory: String): BitmapDescriptor {
        val pinBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_map_marker_empty)
        val bitmap = Bitmap.createBitmap(pinBitmap.width, pinBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(pinBitmap, 0f, 0f, Paint())

        val iconResourceId = when (placeCategory.toLowerCase()) {
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

        if (iconResourceId != null) {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = Color.WHITE
            canvas.drawCircle(bitmap.width.toFloat() / 2, bitmap.height.toFloat() * 0.43f, bitmap.width.toFloat() * 0.27f, paint)
            val dst = RectF(bitmap.width.toFloat() * 0.3f, bitmap.width.toFloat() * 0.23f, bitmap.width.toFloat() * 0.7f, bitmap.height.toFloat() * 0.63f)
            val dstInt = Rect(dst.left.toInt(), dst.top.toInt(), dst.right.toInt(), dst.bottom.toInt())
            val iconBitmap = toBitmap(iconResourceId, dstInt.right - dstInt.left, dstInt.bottom - dstInt.top)
            paint.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.primary_dark), PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(iconBitmap, null, dstInt, paint)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun toBitmap(@DrawableRes drawableId: Int, preferredWidth: Int, preferredHeight: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)

        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else if (drawable is VectorDrawable || drawable is VectorDrawableCompat) {
            val bitmap = Bitmap.createBitmap(preferredWidth, preferredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } else {
            throw IllegalArgumentException("Unsupported drawable")
        }
    }
}