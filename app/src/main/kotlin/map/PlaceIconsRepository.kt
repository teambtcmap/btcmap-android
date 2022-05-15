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
            if (has("tourism") && this["tourism"].asString == "hotel") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].asString == "hostel") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].asString == "apartment") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].asString == "guest_house") {
                return R.drawable.baseline_hotel_24
            }

            if (has("tourism") && this["tourism"].asString == "gallery") {
                return R.drawable.baseline_palette_24
            }

            if (has("tourism") && this["tourism"].asString == "chalet") {
                return R.drawable.baseline_chalet_24
            }

            if (has("company") && this["company"].asString == "transport") {
                return R.drawable.baseline_directions_car_24
            }

            if (has("shop") && this["shop"].asString == "scuba_diving") {
                return R.drawable.baseline_scuba_diving_24
            }

            if (has("shop") && this["shop"].asString == "computer") {
                return R.drawable.baseline_computer_24
            }

            if (has("shop") && this["shop"].asString == "electronics") {
                return R.drawable.baseline_computer_24
            }

            if (has("shop") && this["shop"].asString == "hardware") {
                return R.drawable.baseline_hardware_24
            }

            if (has("shop") && this["shop"].asString == "hairdresser") {
                return R.drawable.ic_tmp_barbershop
            }

            if (has("shop") && this["shop"].asString == "massage") {
                return R.drawable.baseline_spa_24
            }

            if (has("shop") && this["shop"].asString == "beauty") {
                return R.drawable.baseline_spa_24
            }

            if (has("shop") && this["shop"].asString == "mobile_phone") {
                return R.drawable.baseline_smartphone_24
            }

            if (has("shop") && this["shop"].asString == "supermarket") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].asString == "wholesale") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].asString == "interior_decoration") {
                return R.drawable.baseline_design_services_24
            }

            if (has("shop") && this["shop"].asString == "video_games") {
                return R.drawable.baseline_games_24
            }

            if (has("shop") && this["shop"].asString == "jewelry") {
                return R.drawable.baseline_diamond_24
            }

            if (has("shop") && this["shop"].asString == "e-cigarette") {
                return R.drawable.baseline_vaping_rooms_24
            }

            if (has("shop") && this["shop"].asString == "clothes") {
                return R.drawable.baseline_storefront_24
            }

            if (has("shop") && this["shop"].asString == "yes") {
                return R.drawable.baseline_storefront_24
            }

            if (has("shop") && this["shop"].asString == "car_parts") {
                return R.drawable.baseline_directions_car_24
            }

            if (has("shop") && this["shop"].asString == "car_repair") {
                return R.drawable.baseline_car_repair_24
            }

            if (has("shop") && this["shop"].asString == "deli") {
                return R.drawable.baseline_tapas_24
            }

            if (has("shop") && this["shop"].asString == "watches") {
                return R.drawable.baseline_watch_24
            }

            if (has("shop") && this["shop"].asString == "florist") {
                return R.drawable.baseline_local_florist_24
            }

            if (has("shop") && this["shop"].asString == "storage_rental") {
                return R.drawable.baseline_warehouse_24
            }

            if (has("shop") && this["shop"].asString == "garden_centre") {
                return R.drawable.baseline_local_florist_24
            }

            if (has("shop") && this["shop"].asString == "toys") {
                return R.drawable.baseline_toys_24
            }

            if (has("shop") && this["shop"].asString == "sports") {
                return R.drawable.baseline_sports_24
            }

            if (has("shop") && this["shop"].asString == "convenience") {
                return R.drawable.baseline_local_grocery_store_24
            }

            if (has("shop") && this["shop"].asString == "travel_agency") {
                return R.drawable.baseline_luggage_24
            }

            if (has("cuisine") && this["cuisine"].asString == "burger") {
                return R.drawable.baseline_lunch_dining_24
            }

            if (has("cuisine") && this["cuisine"].asString == "pizza") {
                return R.drawable.baseline_local_pizza_24
            }

            if (has("amenity") && this["amenity"].asString == "bar") {
                return R.drawable.baseline_local_bar_24
            }

            if (has("amenity") && this["amenity"].asString == "restaurant") {
                return R.drawable.baseline_restaurant_24
            }

            if (has("amenity") && this["amenity"].asString.lowercase() == "spa") {
                return R.drawable.baseline_spa_24
            }

            if (has("amenity") && this["amenity"].asString == "training") {
                return R.drawable.baseline_school_24
            }

            if (has("amenity") && this["amenity"].asString == "bureau_de_change") {
                return R.drawable.baseline_currency_exchange_24
            }

            if (has("amenity") && this["amenity"].asString == "car_wash") {
                return R.drawable.baseline_local_car_wash_24
            }

            if (has("amenity") && this["amenity"].asString == "atm") {
                return R.drawable.baseline_local_atm_24
            }

            if (has("amenity") && this["amenity"].asString == "cafe") {
                return R.drawable.baseline_local_cafe_24
            }

            if (has("amenity") && this["amenity"].asString == "pub") {
                return R.drawable.baseline_sports_bar_24
            }

            if (has("office") && this["office"].asString == "lawyer") {
                return R.drawable.ic_tmp_scales
            }

            if (has("office") && this["office"].asString == "company") {
                return R.drawable.baseline_business_24
            }

            if (has("office") && this["office"].asString == "it") {
                return R.drawable.baseline_computer_24
            }

            if (has("office") && this["office"].asString == "educational_institution") {
                return R.drawable.baseline_school_24
            }

            if (has("office") && this["office"].asString == "graphic_design") {
                return R.drawable.baseline_design_services_24
            }

            if (has("office") && this["office"].asString == "marketing") {
                return R.drawable.baseline_business_24
            }

            if (has("office") && this["office"].asString == "limousine_service") {
                return R.drawable.baseline_local_taxi_24
            }

            if (has("office") && this["office"].asString == "coworking") {
                return R.drawable.baseline_business_24
            }

            if (has("leisure") && this["leisure"].asString == "fitness_centre") {
                return R.drawable.baseline_fitness_center_24
            }

            if (has("healthcare") && this["healthcare"].asString == "dentist") {
                return R.drawable.baseline_medical_services_24
            }

            if (has("healthcare") && this["healthcare"].asString == "clinic") {
                return R.drawable.baseline_medical_services_24
            }

            if (has("healthcare") && this["healthcare"].asString == "pharmacy") {
                return R.drawable.baseline_local_pharmacy_24
            }

            if (has("building") && this["building"].asString == "commercial") {
                return R.drawable.baseline_business_24
            }
        }

        return null
    }
}