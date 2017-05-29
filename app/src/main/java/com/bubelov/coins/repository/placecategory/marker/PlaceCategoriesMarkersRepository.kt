package com.bubelov.coins.repository.placecategory.marker

import android.content.Context
import android.graphics.*
import com.bubelov.coins.R
import com.bubelov.coins.model.PlaceCategory
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository
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
class PlaceCategoriesMarkersRepository @Inject
internal constructor(private val context: Context, private val categoriesRepository: PlaceCategoriesRepository) {
    private val cache = mutableMapOf<PlaceCategory?, BitmapDescriptor>()

    fun getPlaceCategoryMarker(categoryId: Long): BitmapDescriptor {
        val placeCategory = categoriesRepository.getPlaceCategory(categoryId)
        var marker = cache[placeCategory]

        if (marker == null) {
            marker = createBitmapDescriptor(placeCategory)
            cache[placeCategory] = marker
        }

        return marker
    }

    private fun createBitmapDescriptor(placeCategory: PlaceCategory?): BitmapDescriptor {
        val pinBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_map_marker_empty)
        val bitmap = Bitmap.createBitmap(pinBitmap.width, pinBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(pinBitmap, 0f, 0f, Paint())

        if (placeCategory == null) {
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        val iconResourceId: Int?

        when (placeCategory.name.toLowerCase()) {
            "atm" -> iconResourceId = R.drawable.ic_atm
            "restaurant" -> iconResourceId = R.drawable.ic_restaurant
            "café" -> iconResourceId = R.drawable.ic_cafe
            "bar" -> iconResourceId = R.drawable.ic_bar
            "hotel" -> iconResourceId = R.drawable.ic_hotel
            "pizza" -> iconResourceId = R.drawable.ic_pizza
            "fast food" -> iconResourceId = R.drawable.ic_fast_food
            "hospital" -> iconResourceId = R.drawable.ic_hospital
            "pharmacy" -> iconResourceId = R.drawable.ic_pharmacy
            "taxi" -> iconResourceId = R.drawable.ic_taxi
            "gas station" -> iconResourceId = R.drawable.ic_gas_station
            else -> iconResourceId = null
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

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        } else if (drawable is VectorDrawable || drawable is VectorDrawableCompat) {
            val bitmap = Bitmap.createBitmap(preferredWidth, preferredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        } else {
            throw IllegalArgumentException("Unsupported drawable")
        }
    }
}