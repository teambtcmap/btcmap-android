package map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorFilter
import androidx.core.graphics.toRect
import org.btcmap.R
import db.Place

class PlaceIconsRepository(
    private val context: Context
) {

    private val markersCache = mutableMapOf<Int?, Bitmap>()
    private val markerDrawablesCache = mutableMapOf<Int?, BitmapDrawable>()

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
            Color.parseColor("#ff9100")
        )

        isAntiAlias = true
    }

    fun getMarker(place: Place): Bitmap {
        val iconResId = getIconResId(place)
        var marker = markersCache[iconResId]

        if (marker == null) {
            marker = createMarker(place)
            markersCache[iconResId] = marker
        }

        return createMarker(place)
    }

    fun getMarkerDrawable(place: Place): BitmapDrawable {
        val iconResId = getIconResId(place)
        var markerDrawable = markerDrawablesCache[iconResId]

        if (markerDrawable == null) {
            markerDrawable = createMarker(place).toDrawable(context.resources)
            markerDrawablesCache[iconResId] = markerDrawable
        }

        return createMarker(place).toDrawable(context.resources)
    }

    fun getPlaceIcon(place: Place): Bitmap {
        val iconId = getIconResId(place) ?: R.drawable.ic_place
        return ContextCompat.getDrawable(context, iconId)!!.toBitmap()
    }

    private fun getIconResId(place: Place): Int? {
        if (place.tags.has("tourism") && place.tags["tourism"].asString == "hotel") {
            return R.drawable.ic_hotel
        }

        if (place.tags.has("tourism") && place.tags["tourism"].asString == "hostel") {
            return R.drawable.ic_hotel
        }

        if (place.tags.has("tourism") && place.tags["tourism"].asString == "apartment") {
            return R.drawable.ic_hotel
        }

        if (place.tags.has("tourism") && place.tags["tourism"].asString == "guest_house") {
            return R.drawable.ic_hotel
        }

        if (place.tags.has("tourism") && place.tags["tourism"].asString == "gallery") {
            return R.drawable.baseline_palette_24
        }

        if (place.tags.has("company") && place.tags["company"].asString == "transport") {
            return R.drawable.baseline_directions_car_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "scuba_diving") {
            return R.drawable.baseline_scuba_diving_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "computer") {
            return R.drawable.baseline_computer_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "electronics") {
            return R.drawable.baseline_computer_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "hardware") {
            return R.drawable.baseline_hardware_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "hairdresser") {
            return R.drawable.ic_tmp_barbershop
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "massage") {
            return R.drawable.baseline_spa_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "beauty") {
            return R.drawable.baseline_spa_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "mobile_phone") {
            return R.drawable.baseline_smartphone_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "supermarket") {
            return R.drawable.baseline_local_grocery_store_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "wholesale") {
            return R.drawable.baseline_local_grocery_store_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "interior_decoration") {
            return R.drawable.baseline_design_services_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "video_games") {
            return R.drawable.baseline_games_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "jewelry") {
            return R.drawable.baseline_diamond_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "e-cigarette") {
            return R.drawable.baseline_vaping_rooms_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "clothes") {
            return R.drawable.baseline_storefront_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "yes") {
            return R.drawable.baseline_storefront_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "car_parts") {
            return R.drawable.baseline_directions_car_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "car_repair") {
            return R.drawable.baseline_car_repair_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "deli") {
            return R.drawable.baseline_tapas_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "watches") {
            return R.drawable.baseline_watch_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "florist") {
            return R.drawable.baseline_local_florist_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "storage_rental") {
            return R.drawable.baseline_warehouse_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "garden_centre") {
            return R.drawable.baseline_local_florist_24
        }

        if (place.tags.has("shop") && place.tags["shop"].asString == "toys") {
            return R.drawable.baseline_toys_24
        }

        if (place.tags.has("cuisine") && place.tags["cuisine"].asString == "burger") {
            return R.drawable.baseline_lunch_dining_24
        }

        if (place.tags.has("cuisine") && place.tags["cuisine"].asString == "pizza") {
            return R.drawable.baseline_local_pizza_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "bar") {
            return R.drawable.baseline_local_bar_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "restaurant") {
            return R.drawable.ic_restaurant
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString.lowercase() == "spa") {
            return R.drawable.baseline_spa_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "training") {
            return R.drawable.baseline_school_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "bureau_de_change") {
            return R.drawable.baseline_currency_exchange_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "car_wash") {
            return R.drawable.baseline_local_car_wash_24
        }

        if (place.tags.has("amenity") && place.tags["amenity"].asString == "atm") {
            return R.drawable.ic_atm
        }

        if (place.tags.has("office") && place.tags["office"].asString == "lawyer") {
            return R.drawable.ic_tmp_scales
        }

        if (place.tags.has("office") && place.tags["office"].asString == "company") {
            return R.drawable.baseline_business_24
        }

        if (place.tags.has("office") && place.tags["office"].asString == "it") {
            return R.drawable.baseline_computer_24
        }

        if (place.tags.has("office") && place.tags["office"].asString == "educational_institution") {
            return R.drawable.baseline_school_24
        }

        if (place.tags.has("office") && place.tags["office"].asString == "graphic_design") {
            return R.drawable.baseline_design_services_24
        }

        if (place.tags.has("office") && place.tags["office"].asString == "marketing") {
            return R.drawable.baseline_business_24
        }

        if (place.tags.has("office") && place.tags["office"].asString == "limousine_service") {
            return R.drawable.ic_taxi
        }

        if (place.tags.has("office") && place.tags["office"].asString == "coworking") {
            return R.drawable.baseline_business_24
        }

        if (place.tags.has("leisure") && place.tags["leisure"].asString == "fitness_centre") {
            return R.drawable.baseline_fitness_center_24
        }

        if (place.tags.has("healthcare") && place.tags["healthcare"].asString == "dentist") {
            return R.drawable.baseline_medical_services_24
        }

        if (place.tags.has("healthcare") && place.tags["healthcare"].asString == "clinic") {
            return R.drawable.baseline_medical_services_24
        }

        if (place.tags.has("healthcare") && place.tags["healthcare"].asString == "pharmacy") {
            return R.drawable.baseline_local_pharmacy_24
        }

        return null
    }

    private fun createMarker(place: Place): Bitmap {
        val iconResId = getIconResId(place) ?: return createBitmap(
            emptyPinBitmap.width,
            emptyPinBitmap.height
        ).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        return createMarker(iconResId)
    }

    fun createMarker(drawableId: Int): Bitmap {
        val pinBitmap = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

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
            drawableId,
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
        } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is VectorDrawable) || drawable is VectorDrawableCompat) {
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