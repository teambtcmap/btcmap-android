package icons

import androidx.annotation.DrawableRes
import db.Place
import org.btcmap.R

@DrawableRes
fun Place.iconResId(): Int? {
    tags.apply {
        return when {
            opt("tourism") == "hotel" -> R.drawable.baseline_hotel_24
            opt("tourism") == "hostel" -> R.drawable.baseline_hotel_24
            opt("tourism") == "apartment" -> R.drawable.baseline_hotel_24
            opt("tourism") == "guest_house" -> R.drawable.baseline_hotel_24
            opt("tourism") == "gallery" -> R.drawable.baseline_palette_24
            opt("tourism") == "chalet" -> R.drawable.baseline_chalet_24
            opt("tourism") == "motel" -> R.drawable.baseline_hotel_24
            opt("company") == "transport" -> R.drawable.baseline_directions_car_24
            opt("shop") == "scuba_diving" -> R.drawable.baseline_scuba_diving_24
            opt("shop") == "computer" -> R.drawable.baseline_computer_24
            opt("shop") == "electronics" -> R.drawable.baseline_computer_24
            opt("shop") == "hardware" -> R.drawable.baseline_hardware_24
            opt("shop") == "hairdresser" -> R.drawable.baseline_content_cut_24
            opt("shop") == "massage" -> R.drawable.baseline_spa_24
            opt("shop") == "beauty" -> R.drawable.baseline_spa_24
            opt("shop") == "mobile_phone" -> R.drawable.baseline_smartphone_24
            opt("shop") == "supermarket" -> R.drawable.baseline_local_grocery_store_24
            opt("shop") == "wholesale" -> R.drawable.baseline_local_grocery_store_24
            opt("shop") == "interior_decoration" -> R.drawable.baseline_design_services_24
            opt("shop") == "video_games" -> R.drawable.baseline_games_24
            opt("shop") == "jewelry" -> R.drawable.baseline_diamond_24
            opt("shop") == "e-cigarette" -> R.drawable.baseline_vaping_rooms_24
            opt("shop") == "clothes" -> R.drawable.baseline_storefront_24
            opt("shop") == "yes" -> R.drawable.baseline_storefront_24
            opt("shop") == "car_parts" -> R.drawable.baseline_directions_car_24
            opt("shop") == "car_repair" -> R.drawable.baseline_car_repair_24
            opt("shop") == "deli" -> R.drawable.baseline_tapas_24
            opt("shop") == "watches" -> R.drawable.baseline_watch_24
            opt("shop") == "florist" -> R.drawable.baseline_local_florist_24
            opt("shop") == "storage_rental" -> R.drawable.baseline_warehouse_24
            opt("shop") == "garden_centre" -> R.drawable.baseline_local_florist_24
            opt("shop") == "toys" -> R.drawable.baseline_toys_24
            opt("shop") == "sports" -> R.drawable.baseline_sports_24
            opt("shop") == "convenience" -> R.drawable.baseline_local_grocery_store_24
            opt("shop") == "travel_agency" -> R.drawable.baseline_luggage_24
            opt("shop") == "laundry" -> R.drawable.baseline_local_laundry_service_24
            opt("shop") == "surf" -> R.drawable.baseline_surfing_24
            opt("shop") == "video" -> R.drawable.baseline_local_movies_24
            opt("cuisine") == "burger" -> R.drawable.baseline_lunch_dining_24
            opt("cuisine") == "pizza" -> R.drawable.baseline_local_pizza_24
            opt("amenity") == "bar" -> R.drawable.baseline_local_bar_24
            opt("amenity") == "restaurant" -> R.drawable.baseline_restaurant_24
            opt("amenity") == "spa" -> R.drawable.baseline_spa_24
            opt("amenity") == "training" -> R.drawable.baseline_school_24
            opt("amenity") == "bureau_de_change" -> R.drawable.baseline_currency_exchange_24
            opt("amenity") == "car_wash" -> R.drawable.baseline_local_car_wash_24
            opt("amenity") == "atm" -> R.drawable.baseline_local_atm_24
            opt("amenity") == "cafe" -> R.drawable.baseline_local_cafe_24
            opt("amenity") == "pub" -> R.drawable.baseline_sports_bar_24
            opt("office") == "lawyer" -> R.drawable.baseline_balance_24
            opt("office") == "company" -> R.drawable.baseline_business_24
            opt("office") == "it" -> R.drawable.baseline_computer_24
            opt("office") == "educational_institution" -> R.drawable.baseline_school_24
            opt("office") == "graphic_design" -> R.drawable.baseline_design_services_24
            opt("office") == "marketing" -> R.drawable.baseline_business_24
            opt("office") == "limousine_service" -> R.drawable.baseline_local_taxi_24
            opt("office") == "coworking" -> R.drawable.baseline_business_24
            opt("leisure") == "fitness_centre" -> R.drawable.baseline_fitness_center_24
            opt("healthcare") == "dentist" -> R.drawable.baseline_medical_services_24
            opt("healthcare") == "clinic" -> R.drawable.baseline_medical_services_24
            opt("healthcare") == "pharmacy" -> R.drawable.baseline_local_pharmacy_24
            opt("building") == "commercial" -> R.drawable.baseline_business_24
            else -> null
        }
    }
}