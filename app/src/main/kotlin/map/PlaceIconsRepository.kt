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
import com.google.gson.JsonObject
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

    @JvmInline
    private value class JsonObjectWrapper(private val value: JsonObject) {
        operator fun get(key: String): String? = if (value.has(key)) value[key].asString else null
    }

    @DrawableRes
    private fun Place.iconResId(): Int? {
        val tags = JsonObjectWrapper(tags)
        return when {
            tags["tourism"] == "hotel" -> R.drawable.baseline_hotel_24
            tags["tourism"] == "hostel" -> R.drawable.baseline_hotel_24
            tags["tourism"] == "apartment" -> R.drawable.baseline_hotel_24
            tags["tourism"] == "guest_house" -> R.drawable.baseline_hotel_24
            tags["tourism"] == "gallery" -> R.drawable.baseline_palette_24
            tags["tourism"] == "chalet" -> R.drawable.baseline_chalet_24
            tags["tourism"] == "motel" -> R.drawable.baseline_hotel_24
            tags["company"] == "transport" -> R.drawable.baseline_directions_car_24
            tags["shop"] == "scuba_diving" -> R.drawable.baseline_scuba_diving_24
            tags["shop"] == "computer" -> R.drawable.baseline_computer_24
            tags["shop"] == "electronics" -> R.drawable.baseline_computer_24
            tags["shop"] == "hardware" -> R.drawable.baseline_hardware_24
            tags["shop"] == "hairdresser" -> R.drawable.ic_tmp_barbershop
            tags["shop"] == "massage" -> R.drawable.baseline_spa_24
            tags["shop"] == "beauty" -> R.drawable.baseline_spa_24
            tags["shop"] == "mobile_phone" -> R.drawable.baseline_smartphone_24
            tags["shop"] == "supermarket" -> R.drawable.baseline_local_grocery_store_24
            tags["shop"] == "wholesale" -> R.drawable.baseline_local_grocery_store_24
            tags["shop"] == "interior_decoration" -> R.drawable.baseline_design_services_24
            tags["shop"] == "video_games" -> R.drawable.baseline_games_24
            tags["shop"] == "jewelry" -> R.drawable.baseline_diamond_24
            tags["shop"] == "e-cigarette" -> R.drawable.baseline_vaping_rooms_24
            tags["shop"] == "clothes" -> R.drawable.baseline_storefront_24
            tags["shop"] == "yes" -> R.drawable.baseline_storefront_24
            tags["shop"] == "car_parts" -> R.drawable.baseline_directions_car_24
            tags["shop"] == "car_repair" -> R.drawable.baseline_car_repair_24
            tags["shop"] == "deli" -> R.drawable.baseline_tapas_24
            tags["shop"] == "watches" -> R.drawable.baseline_watch_24
            tags["shop"] == "florist" -> R.drawable.baseline_local_florist_24
            tags["shop"] == "storage_rental" -> R.drawable.baseline_warehouse_24
            tags["shop"] == "garden_centre" -> R.drawable.baseline_local_florist_24
            tags["shop"] == "toys" -> R.drawable.baseline_toys_24
            tags["shop"] == "sports" -> R.drawable.baseline_sports_24
            tags["shop"] == "convenience" -> R.drawable.baseline_local_grocery_store_24
            tags["shop"] == "travel_agency" -> R.drawable.baseline_luggage_24
            tags["cuisine"] == "burger" -> R.drawable.baseline_lunch_dining_24
            tags["cuisine"] == "pizza" -> R.drawable.baseline_local_pizza_24
            tags["amenity"] == "bar" -> R.drawable.baseline_local_bar_24
            tags["amenity"] == "restaurant" -> R.drawable.baseline_restaurant_24
            tags["amenity"]?.lowercase() == "spa" -> R.drawable.baseline_spa_24
            tags["amenity"] == "training" -> R.drawable.baseline_school_24
            tags["amenity"] == "bureau_de_change" -> R.drawable.baseline_currency_exchange_24
            tags["amenity"] == "car_wash" -> R.drawable.baseline_local_car_wash_24
            tags["amenity"] == "atm" -> R.drawable.baseline_local_atm_24
            tags["amenity"] == "cafe" -> R.drawable.baseline_local_cafe_24
            tags["amenity"] == "pub" -> R.drawable.baseline_sports_bar_24
            tags["office"] == "lawyer" -> R.drawable.ic_tmp_scales
            tags["office"] == "company" -> R.drawable.baseline_business_24
            tags["office"] == "it" -> R.drawable.baseline_computer_24
            tags["office"] == "educational_institution" -> R.drawable.baseline_school_24
            tags["office"] == "graphic_design" -> R.drawable.baseline_design_services_24
            tags["office"] == "marketing" -> R.drawable.baseline_business_24
            tags["office"] == "limousine_service" -> R.drawable.baseline_local_taxi_24
            tags["office"] == "coworking" -> R.drawable.baseline_business_24
            tags["leisure"] == "fitness_centre" -> R.drawable.baseline_fitness_center_24
            tags["healthcare"] == "dentist" -> R.drawable.baseline_medical_services_24
            tags["healthcare"] == "clinic" -> R.drawable.baseline_medical_services_24
            tags["healthcare"] == "pharmacy" -> R.drawable.baseline_local_pharmacy_24
            tags["building"] == "commercial" -> R.drawable.baseline_business_24
            else -> null
        }
    }
}