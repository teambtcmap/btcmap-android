package map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
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
import org.koin.core.annotation.Single

@Single
class PlaceIconsRepository(
    private val context: Context,
) {

    private val iconCache = mutableMapOf<Int, Bitmap>()
    private val markerIconCache = mutableMapOf<Int?, BitmapDrawable>()

    private val emptyPinBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.ic_map_marker_empty,
    )

    private val pinCirclePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val pinIconPaint = Paint().apply {
        colorFilter = PorterDuff.Mode.SRC_IN.toColorFilter(Color.parseColor("#ff9100"))
        isAntiAlias = true
    }

    fun getIcon(place: Place): Bitmap {
        val iconResId = place.iconResId() ?: R.drawable.ic_place

        var icon = iconCache[iconResId]

        if (icon == null) {
            icon = ContextCompat.getDrawable(context, iconResId)!!.toBitmap()
            iconCache[iconResId] = icon
        }

        return icon
    }

    fun getMarkerIcon(place: Place): BitmapDrawable {
        val iconResId = place.iconResId()
        var markerDrawable = markerIconCache[iconResId]

        if (markerDrawable == null) {
            markerDrawable = createMarkerIcon(iconResId).toDrawable(context.resources)
            markerIconCache[iconResId] = markerDrawable
        }

        return markerDrawable
    }

    private fun createMarkerIcon(iconResId: Int?): Bitmap {
        val markerIcon = createBitmap(emptyPinBitmap.width, emptyPinBitmap.height).applyCanvas {
            drawBitmap(emptyPinBitmap, 0f, 0f, Paint())
        }

        if (iconResId != null) {
            markerIcon.applyCanvas {
                drawCircle(
                    markerIcon.width.toFloat() / 2,
                    markerIcon.height.toFloat() * 0.43f,
                    markerIcon.width.toFloat() * 0.27f,
                    pinCirclePaint
                )
            }

            val iconFrame = RectF(
                markerIcon.width.toFloat() * 0.3f,
                markerIcon.width.toFloat() * 0.23f,
                markerIcon.width.toFloat() * 0.7f,
                markerIcon.height.toFloat() * 0.63f
            ).toRect()

            val iconBitmap = toBitmap(
                iconResId,
                iconFrame.right - iconFrame.left,
                iconFrame.bottom - iconFrame.top,
            )

            markerIcon.applyCanvas {
                drawBitmap(iconBitmap, null, iconFrame, pinIconPaint)
            }
        }

        return markerIcon
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

    private fun Place.iconResId(): Int? {
        tags.apply {
            if (has("tourism") && this["tourism"].toString() == "hotel") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].toString() == "hostel") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].toString() == "apartment") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].toString() == "guest_house") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].toString() == "gallery") {
                return R.drawable.baseline_palette_24
            }

            if (has("tourism") && this["tourism"].toString() == "chalet") {
                return R.drawable.baseline_chalet_24
            }

            if (has("company") && this["company"].toString() == "transport") {
                return R.drawable.baseline_directions_car_24
            }

            if (has("shop") && this["shop"].toString() == "scuba_diving") {
                return R.drawable.baseline_scuba_diving_24
            }

            if (has("shop") && this["shop"].toString() == "computer") {
                return R.drawable.baseline_computer_24
            }

            if (has("shop") && this["shop"].toString() == "electronics") {
                return R.drawable.baseline_computer_24
            }

            if (has("shop") && this["shop"].toString() == "hardware") {
                return R.drawable.baseline_hardware_24
            }

            if (has("shop") && this["shop"].toString() == "hairdresser") {
                return R.drawable.ic_tmp_barbershop
            }

            if (has("shop") && this["shop"].toString() == "massage") {
                return R.drawable.baseline_spa_24
            }

            if (has("shop") && this["shop"].toString() == "beauty") {
                return R.drawable.baseline_spa_24
            }

            if (has("shop") && this["shop"].toString() == "mobile_phone") {
                return R.drawable.baseline_smartphone_24
            }

            if (has("shop") && this["shop"].toString() == "supermarket") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].toString() == "wholesale") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].toString() == "interior_decoration") {
                return R.drawable.baseline_design_services_24
            }

            if (has("shop") && this["shop"].toString() == "video_games") {
                return R.drawable.baseline_games_24
            }

            if (has("shop") && this["shop"].toString() == "jewelry") {
                return R.drawable.baseline_diamond_24
            }

            if (has("shop") && this["shop"].toString() == "e-cigarette") {
                return R.drawable.baseline_vaping_rooms_24
            }

            if (has("shop") && this["shop"].toString() == "clothes") {
                return R.drawable.baseline_storefront_24
            }

            if (has("shop") && this["shop"].toString() == "yes") {
                return R.drawable.baseline_storefront_24
            }

            if (has("shop") && this["shop"].toString() == "car_parts") {
                return R.drawable.baseline_directions_car_24
            }

            if (has("shop") && this["shop"].toString() == "car_repair") {
                return R.drawable.baseline_car_repair_24
            }

            if (has("shop") && this["shop"].toString() == "deli") {
                return R.drawable.baseline_tapas_24
            }

            if (has("shop") && this["shop"].toString() == "watches") {
                return R.drawable.baseline_watch_24
            }

            if (has("shop") && this["shop"].toString() == "florist") {
                return R.drawable.baseline_local_florist_24
            }

            if (has("shop") && this["shop"].toString() == "storage_rental") {
                return R.drawable.baseline_warehouse_24
            }

            if (has("shop") && this["shop"].toString() == "garden_centre") {
                return R.drawable.baseline_local_florist_24
            }

            if (has("shop") && this["shop"].toString() == "toys") {
                return R.drawable.baseline_toys_24
            }

            if (has("shop") && this["shop"].toString() == "sports") {
                return R.drawable.baseline_sports_24
            }

            if (has("shop") && this["shop"].toString() == "convenience") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].toString() == "travel_agency") {
                return R.drawable.baseline_luggage_24
            }

            if (has("cuisine") && this["cuisine"].toString() == "burger") {
                return R.drawable.baseline_lunch_dining_24
            }

            if (has("cuisine") && this["cuisine"].toString() == "pizza") {
                return R.drawable.baseline_local_pizza_24
            }

            if (has("amenity") && this["amenity"].toString() == "bar") {
                return R.drawable.baseline_local_bar_24
            }

            if (has("amenity") && this["amenity"].toString() == "restaurant") {
                return R.drawable.baseline_restaurant_24
            }

            if (has("amenity") && this["amenity"].toString().lowercase() == "spa") {
                return R.drawable.baseline_spa_24
            }

            if (has("amenity") && this["amenity"].toString() == "training") {
                return R.drawable.baseline_school_24
            }

            if (has("amenity") && this["amenity"].toString() == "bureau_de_change") {
                return R.drawable.baseline_currency_exchange_24
            }

            if (has("amenity") && this["amenity"].toString() == "car_wash") {
                return R.drawable.baseline_local_car_wash_24
            }

            if (has("amenity") && this["amenity"].toString() == "atm") {
                return R.drawable.baseline_local_atm_24
            }

            if (has("amenity") && this["amenity"].toString() == "cafe") {
                return R.drawable.baseline_local_cafe_24
            }

            if (has("amenity") && this["amenity"].toString() == "pub") {
                return R.drawable.baseline_sports_bar_24
            }

            if (has("office") && this["office"].toString() == "lawyer") {
                return R.drawable.ic_tmp_scales
            }

            if (has("office") && this["office"].toString() == "company") {
                return R.drawable.baseline_business_24
            }

            if (has("office") && this["office"].toString() == "it") {
                return R.drawable.baseline_computer_24
            }

            if (has("office") && this["office"].toString() == "educational_institution") {
                return R.drawable.baseline_school_24
            }

            if (has("office") && this["office"].toString() == "graphic_design") {
                return R.drawable.baseline_design_services_24
            }

            if (has("office") && this["office"].toString() == "marketing") {
                return R.drawable.baseline_business_24
            }

            if (has("office") && this["office"].toString() == "limousine_service") {
                return R.drawable.baseline_local_taxi_24
            }

            if (has("office") && this["office"].toString() == "coworking") {
                return R.drawable.baseline_business_24
            }

            if (has("leisure") && this["leisure"].toString() == "fitness_centre") {
                return R.drawable.baseline_fitness_center_24
            }

            if (has("healthcare") && this["healthcare"].toString() == "dentist") {
                return R.drawable.baseline_medical_services_24
            }

            if (has("healthcare") && this["healthcare"].toString() == "clinic") {
                return R.drawable.baseline_medical_services_24
            }

            if (has("healthcare") && this["healthcare"].toString() == "pharmacy") {
                return R.drawable.baseline_local_pharmacy_24
            }

            if (has("building") && this["building"].toString() == "commercial") {
                return R.drawable.baseline_business_24
            }
        }

        return null
    }
}