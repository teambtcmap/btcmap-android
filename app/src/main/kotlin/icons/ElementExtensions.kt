package icons

import androidx.annotation.DrawableRes
import kotlinx.serialization.json.*
import org.btcmap.R

@DrawableRes
fun String.toIconResId(): Int? {
    return when (this) {
        "icecream" -> R.drawable.baseline_icecream_24
        "hotel" -> R.drawable.baseline_hotel_24
        "tour" -> R.drawable.baseline_tour_24
        "chalet" -> R.drawable.baseline_chalet_24
        "holiday_village" -> R.drawable.baseline_holiday_village_24
        "palette" -> R.drawable.baseline_palette_24
        "info_outline" -> R.drawable.baseline_info_outline_24
        "museum" -> R.drawable.baseline_museum_24
        "computer" -> R.drawable.baseline_computer_24
        "storefront" -> R.drawable.baseline_storefront_24
        "diamond" -> R.drawable.baseline_diamond_24
        "content_cut" -> R.drawable.baseline_content_cut_24
        "local_grocery_store" -> R.drawable.baseline_local_grocery_store_24
        "car_repair" -> R.drawable.baseline_car_repair_24
        "spa" -> R.drawable.baseline_spa_24
        "menu_book" -> R.drawable.baseline_menu_book_24
        "chair" -> R.drawable.baseline_chair_24
        "card_giftcard" -> R.drawable.baseline_card_giftcard_24
        "luggage" -> R.drawable.baseline_luggage_24
        "smartphone" -> R.drawable.baseline_smartphone_24
        "smoking_rooms" -> R.drawable.baseline_smoking_rooms_24
        "directions_car" -> R.drawable.baseline_directions_car_24
        "bakery_dining" -> R.drawable.baseline_bakery_dining_24
        "local_florist" -> R.drawable.baseline_local_florist_24
        "pedal_bike" -> R.drawable.baseline_pedal_bike_24
        "vaping_rooms" -> R.drawable.baseline_vaping_rooms_24
        "visibility" -> R.drawable.baseline_visibility_24
        "photo_camera" -> R.drawable.baseline_photo_camera_24
        "tapas" -> R.drawable.baseline_tapas_24
        "sports" -> R.drawable.baseline_sports_24
        "music_note" -> R.drawable.baseline_music_note_24
        "hardware" -> R.drawable.baseline_hardware_24
        "local_printshop" -> R.drawable.baseline_local_printshop_24
        "wine_bar" -> R.drawable.baseline_wine_bar_24
        "liquor" -> R.drawable.baseline_liquor_24
        "toys" -> R.drawable.baseline_toys_24
        "attach_money" -> R.drawable.baseline_attach_money_24
        "checkroom" -> R.drawable.baseline_checkroom_24
        "restaurant" -> R.drawable.baseline_restaurant_24
        "pool" -> R.drawable.baseline_pool_24
        "two_wheeler" -> R.drawable.baseline_two_wheeler_24
        "scuba_diving" -> R.drawable.baseline_scuba_diving_24
        "lunch_dining" -> R.drawable.baseline_lunch_dining_24
        "local_cafe" -> R.drawable.baseline_local_cafe_24
        "local_bar" -> R.drawable.baseline_local_bar_24
        "currency_exchange" -> R.drawable.baseline_currency_exchange_24
        "business" -> R.drawable.baseline_business_24
        "nightlife" -> R.drawable.baseline_nightlife_24
        "pets" -> R.drawable.baseline_pets_24
        "agriculture" -> R.drawable.baseline_agriculture_24
        "local_atm" -> R.drawable.baseline_local_atm_24
        "balance" -> R.drawable.baseline_balance_24
        "mic" -> R.drawable.baseline_mic_24
        "sports_bar" -> R.drawable.baseline_sports_bar_24
        "account_balance" -> R.drawable.baseline_account_balance_24
        "groups" -> R.drawable.baseline_groups_24
        "surfing" -> R.drawable.baseline_surfing_24
        "local_laundry_service" -> R.drawable.baseline_local_laundry_service_24
        "medical_services" -> R.drawable.baseline_medical_services_24
        "fitness_center" -> R.drawable.baseline_fitness_center_24
        "design_services" -> R.drawable.baseline_design_services_24
        "school" -> R.drawable.baseline_school_24
        "local_hospital" -> R.drawable.baseline_local_hospital_24
        "church" -> R.drawable.baseline_church_24
        "park" -> R.drawable.baseline_park_24
        "directions_boat" -> R.drawable.baseline_directions_boat_24
        "local_mall" -> R.drawable.baseline_local_mall_24
        "beach_access" -> R.drawable.baseline_beach_access_24
        "public" -> R.drawable.baseline_public_24
        "games" -> R.drawable.baseline_games_24
        "lock" -> R.drawable.baseline_lock_24
        "grass" -> R.drawable.baseline_grass_24
        "local_taxi" -> R.drawable.baseline_local_taxi_24
        "local_pharmacy" -> R.drawable.baseline_local_pharmacy_24
        "electrical_services" -> R.drawable.baseline_electrical_services_24
        "adult_content" -> R.drawable.baseline_adult_content_24
        "coffee" -> R.drawable.baseline_coffee_24
        "sailing" -> R.drawable.baseline_sailing_24
        "local_gas_station" -> R.drawable.baseline_local_gas_station_24
        "warehouse" -> R.drawable.baseline_warehouse_24
        "science" -> R.drawable.baseline_science_24
        "hvac" -> R.drawable.baseline_delete_24
        "delete" -> R.drawable.baseline_delete_24
        "edit" -> R.drawable.baseline_edit_24
        "local_car_wash" -> R.drawable.baseline_local_car_wash_24
        "newspaper" -> R.drawable.baseline_newspaper_24
        "home" -> R.drawable.baseline_home_24
        "child_care" -> R.drawable.baseline_child_care_24
        "architecture" -> R.drawable.baseline_architecture_24
        "videocam" -> R.drawable.baseline_videocam_24
        "outdoor_grill" -> R.drawable.baseline_outdoor_grill_24
        "cell_tower" -> R.drawable.baseline_cell_tower_24
        "emoji_food_beverage" -> R.drawable.baseline_emoji_food_beverage_24
        "local_post_office" -> R.drawable.baseline_local_post_office_24
        "flight_takeoff" -> R.drawable.baseline_flight_takeoff_24
        "local_movies" -> R.drawable.baseline_local_movies_24
        "engineering" -> R.drawable.baseline_engineering_24
        "watch" -> R.drawable.baseline_watch_24
        "trip_origin" -> R.drawable.baseline_trip_origin_24
        else -> null
    }
}

fun JsonObject.iconId(): String {
    val tag = fun(tag: String): String? {
        return this[tag]?.jsonPrimitive?.contentOrNull
    }

    return when {
        // Tag combos are more specific so they have a higher prio
        tag("amenity") == "fast_food" && tag("cuisine") == "ice_cream" -> "icecream"

        // 174 elements (22-05-2022)
        tag("tourism") == "hotel" -> "hotel"
        // 130 elements (22-05-2022)
        tag("tourism") == "attraction" -> "tour"
        // 94 elements (22-05-2022)
        tag("tourism") == "guest_house" -> "hotel"
        // 87 elements (22-05-2022)
        tag("tourism") == "apartment" -> "hotel"
        // 40 elements (22-05-2022)
        tag("tourism") == "hostel" -> "hotel"
        // 25 elements (22-05-2022)
        tag("tourism") == "chalet" -> "chalet"
        // 12 elements (22-05-2022)
        tag("tourism") == "camp_site" -> "holiday_village"
        // 7 elements (22-05-2022)
        tag("tourism") == "gallery" -> "palette"
        // 7 elements (22-05-2022)
        tag("tourism") == "artwork" -> "palette"
        // 5 elements (22-05-2022)
        tag("tourism") == "information" -> "info_outline"
        // 5 elements (22-05-2022)
        tag("tourism") == "museum" -> "museum"
        // 3 elements (22-05-2022)
        tag("tourism") == "motel" -> "hotel"

        // 179 elements (31-05-2022)
        tag("shop") == "computer" -> "computer"
        // 124 elements (31-05-2022)
        tag("shop") == "clothes" -> "storefront"
        // 110 elements (31-05-2022)
        tag("shop") == "jewelry" -> "diamond"
        // 89 elements (31-05-2022)
        tag("shop") == "hairdresser" -> "content_cut"
        // 85 elements (31-05-2022)
        tag("shop") == "electronics" -> "computer"
        // 74 elements (31-05-2022)
        tag("shop") == "supermarket" -> "local_grocery_store"
        // 46 elements (31-05-2022)
        tag("shop") == "car_repair" -> "car_repair"
        // 45 elements (31-05-2022)
        tag("shop") == "beauty" -> "spa"
        // 44 elements (31-05-2022)
        tag("shop") == "books" -> "menu_book"
        // 43 elements (31-05-2022)
        tag("shop") == "furniture" -> "chair"
        // 40 elements (31-05-2022)
        tag("shop") == "convenience" -> "local_grocery_store"
        // 34 elements (31-05-2022)
        tag("shop") == "gift" -> "card_giftcard"
        // 33 elements (31-05-2022)
        tag("shop") == "travel_agency" -> "luggage"
        // 33 elements (31-05-2022)
        tag("shop") == "mobile_phone" -> "smartphone"
        // 28 elements (31-05-2022)
        tag("shop") == "tobacco" -> "smoking_rooms"
        // 28 elements (31-05-2022)
        tag("shop") == "car" -> "directions_car"
        // 27 elements (31-05-2022)
        tag("shop") == "bakery" -> "bakery_dining"
        // 25 elements (31-05-2022)
        tag("shop") == "massage" -> "spa"
        // 23 elements (31-05-2022)
        tag("shop") == "florist" -> "local_florist"
        // 23 elements (31-05-2022)
        tag("shop") == "bicycle" -> "pedal_bike"
        // 22 elements (31-05-2022)
        tag("shop") == "e-cigarette" -> "vaping_rooms"
        // 21 elements (31-05-2022)
        tag("shop") == "optician" -> "visibility"
        // 20 elements (31-05-2022)
        tag("shop") == "photo" -> "photo_camera"
        // 20 elements (31-05-2022)
        tag("shop") == "deli" -> "tapas"
        // 19 elements (31-05-2022)
        tag("shop") == "sports" -> "sports"
        // 16 elements (31-05-2022)
        tag("shop") == "farm" -> "storefront"
        // 16 elements (31-05-2022)
        tag("shop") == "art" -> "palette"
        // 15 elements (31-05-2022)
        tag("shop") == "music" -> "music_note"
        // 15 elements (31-05-2022)
        tag("shop") == "hardware" -> "hardware"
        // 15 elements (31-05-2022)
        tag("shop") == "copyshop" -> "local_printshop"
        // 14 elements (31-05-2022)
        tag("shop") == "wine" -> "wine_bar"
        // 14 elements (31-05-2022)
        tag("shop") == "shoes" -> "storefront"
        // 14 elements (31-05-2022)
        tag("shop") == "alcohol" -> "liquor"
        // 13 elements (31-05-2022)
        tag("shop") == "toys" -> "toys"
        // 13 elements (31-05-2022)
        tag("shop") == "greengrocer" -> "storefront"
        // 13 elements (31-05-2022)
        tag("shop") == "car_parts" -> "directions_car"
        // 12 elements (31-05-2022)
        tag("shop") == "tatoo" -> "storefront"
        // 12 elements (31-05-2022)
        tag("shop") == "pawnbroker" -> "attach_money"
        // 12 elements (31-05-2022)
        tag("shop") == "garden_centre" -> "local_florist"
        // 12 elements (31-05-2022)
        tag("shop") == "butcher" -> "storefront"
        // 11 elements (31-05-2022)
        tag("shop") == "variety_store" -> "storefront"
        // 11 elements (31-05-2022)
        tag("shop") == "printing" -> "local_printshop"
        // 11 elements (31-05-2022)
        tag("shop") == "laundry" -> "local_laundry_service"
        // 10 elements (31-05-2022)
        tag("shop") == "kiosk" -> "storefront"
        // 9 elements (31-05-2022)
        tag("shop") == "pet" -> "pets"
        // 9 elements (31-05-2022)
        tag("shop") == "cannabis" -> "grass"
        // 9 elements (31-05-2022)
        tag("shop") == "boutique" -> "storefront"
        // 8 elements (31-05-2022)
        tag("shop") == "stationery" -> "edit"
        // 8 elements (31-05-2022)
        tag("shop") == "pastry" -> "bakery_dining"
        // 8 elements (31-05-2022)
        tag("shop") == "mall" -> "local_mall"
        // 8 elements (31-05-2022)
        tag("shop") == "hifi" -> "music_note"
        // 8 elements (31-05-2022)
        tag("shop") == "estate_agent" -> "home"
        // 8 elements (31-05-2022)
        tag("shop") == "cosmetics" -> "spa"
        // 8 elements (31-05-2022)
        tag("shop") == "coffee" -> "coffee"
        // 7 elements (31-05-2022)
        tag("shop") == "erotic" -> "adult_content"
        // 7 elements (31-05-2022)
        tag("shop") == "confectionery" -> "storefront"
        // 7 elements (31-05-2022)
        tag("shop") == "beverages" -> "liquor"
        // 6 elements (31-05-2022)
        tag("shop") == "video_games" -> "games"
        // 6 elements (31-05-2022)
        tag("shop") == "newsagent" -> "newspaper"
        // 6 elements (31-05-2022)
        tag("shop") == "interior_decoration" -> "design_services"
        // 6 elements (31-05-2022)
        tag("shop") == "electrical" -> "electrical_services"
        // 6 elements (31-05-2022)
        tag("shop") == "doityourself" -> "hardware"
        // 6 elements (31-05-2022)
        tag("shop") == "antiques" -> "storefront"
        // 5 elements (31-05-2022)
        tag("shop") == "watches" -> "watch"
        // 5 elements (31-05-2022)
        tag("shop") == "trade" -> "storefront"
        // 5 elements (31-05-2022)
        tag("shop") == "tea" -> "emoji_food_beverage"
        // 5 elements (31-05-2022)
        tag("shop") == "scuba_diving" -> "scuba_diving"
        // 5 elements (31-05-2022)
        tag("shop") == "musical_instrument" -> "music_note"
        // 5 elements (31-05-2022)
        tag("shop") == "dairy" -> "storefront"
        // 5 elements (31-05-2022)
        tag("shop") == "chocolate" -> "storefront"
        // 5 elements (31-05-2022)
        tag("shop") == "anime" -> "storefront"
        // 4 elements (31-05-2022)
        tag("shop") == "tyres" -> "trip_origin"
        // 4 elements (31-05-2022)
        tag("shop") == "second_hand" -> "storefront"
        // 4 elements (31-05-2022)
        tag("shop") == "perfumery" -> "storefront"
        // 4 elements (31-05-2022)
        tag("shop") == "nutrition_supplements" -> "storefront"
        // 4 elements (31-05-2022)
        tag("shop") == "motorcycle" -> "two_wheeler"
        // 4 elements (31-05-2022)
        tag("shop") == "lottery" -> "storefront"
        // 4 elements (31-05-2022)
        tag("shop") == "locksmith" -> "lock"
        // 4 elements (31-05-2022)
        tag("shop") == "games" -> "games"
        // 4 elements (31-05-2022)
        tag("shop") == "funeral_directors" -> "church"
        // 4 elements (31-05-2022)
        tag("shop") == "department_store" -> "local_mall"
        // 4 elements (31-05-2022)
        tag("shop") == "chemist" -> "science"
        // 3 elements (14-06-2022)
        tag("shop") == "carpet" -> "storefront"
        // 3 elements (31-05-2022)
        tag("shop") == "water_sports" -> "sports"
        // 3 elements (31-05-2022)
        tag("shop") == "water" -> "sports"
        // 3 elements (31-05-2022)
        tag("shop") == "video" -> "videocam"
        // 3 elements (31-05-2022)
        tag("shop") == "tailor" -> "checkroom"
        // 3 elements (31-05-2022)
        tag("shop") == "storage_rental" -> "warehouse"
        // 3 elements (31-05-2022)
        tag("shop") == "storage" -> "warehouse"
        // 3 elements (31-05-2022)
        tag("shop") == "outdoor" -> "outdoor_grill"
        // 3 elements (31-05-2022)
        tag("shop") == "houseware" -> "chair"
        // 3 elements (31-05-2022)
        tag("shop") == "herbalist" -> "local_florist"
        // 3 elements (31-05-2022)
        tag("shop") == "health_food" -> "local_florist"
        // 3 elements (31-05-2022)
        tag("shop") == "grocery" -> "local_grocery_store"
        // 3 elements (31-05-2022)
        tag("shop") == "food" -> "local_grocery_store"
        // 3 elements (31-05-2022)
        tag("shop") == "curtain" -> "storefront"
        // 3 elements (31-05-2022)
        tag("shop") == "boat" -> "sailing"
        // 1 element (31-05-2022)
        tag("shop") == "wholesale" -> "local_grocery_store"
        // 0 elements (31-05-2022)
        tag("shop") == "surf" -> "surfing"
        tag("shop") != null -> "storefront"

        // 674 elements (06-06-2022)
        tag("amenity") == "restaurant" -> "restaurant"
        // 336 elements (06-06-2022)
        tag("amenity") == "atm" -> "local_atm"
        // 199 elements (06-06-2022)
        tag("amenity") == "cafe" -> "local_cafe"
        // 146 elements (06-06-2022)
        tag("amenity") == "bar" -> "local_bar"
        // 100 elements (06-06-2022)
        tag("amenity") == "bureau_de_change" -> "currency_exchange"
        // 87 elements (06-06-2022)
        tag("amenity") == "place_of_worship" -> "church"
        // 77 elements (06-06-2022)
        tag("amenity") == "fast_food" -> "lunch_dining"
        // 70 elements (06-06-2022)
        tag("amenity") == "bank" -> "account_balance"
        // 60 elements (06-06-2022)
        tag("amenity") == "dentist" -> "medical_services"
        // 54 elements (06-06-2022)
        tag("amenity") == "pub" -> "sports_bar"
        // 41 elements (06-06-2022)
        tag("amenity") == "doctors" -> "medical_services"
        // 26 elements (06-06-2022)
        tag("amenity") == "pharmacy" -> "local_pharmacy"
        // 26 elements (06-06-2022)
        tag("amenity") == "clinic" -> "medical_services"
        // 23 elements (06-06-2022)
        tag("amenity") == "school" -> "school"
        // 18 elements (06-06-2022)
        tag("amenity") == "taxi" -> "local_taxi"
        // 16 elements (06-06-2022)
        tag("amenity") == "studio" -> "mic"
        // 16 elements (06-06-2022)
        tag("amenity") == "fuel" -> "local_gas_station"
        // 15 elements (06-06-2022)
        tag("amenity") == "car_rental" -> "directions_car"
        // 11 elements (06-06-2022)
        tag("amenity") == "arts_centre" -> "palette"
        // 11 elements (06-06-2022)
        tag("amenity") == "police" -> "local_police"
        // 10 elements (06-06-2022)
        tag("amenity") == "hospital" -> "local_hospital"
        // 10 elements (06-06-2022)
        tag("amenity") == "brothel" -> "adult_content"
        // 9 elements (06-06-2022)
        tag("amenity") == "veterinary" -> "pets"
        // 8 elements (06-06-2022)
        tag("amenity") == "university" -> "school"
        // 8 elements (06-06-2022)
        tag("amenity") == "college" -> "school"
        // 8 elements (06-06-2022)
        tag("amenity") == "car_wash" -> "local_car_wash"
        // 7 elements (06-06-2022)
        tag("amenity") == "nightclub" -> "nightlife"
        // 7 elements (06-06-2022)
        tag("amenity") == "driving_school" -> "directions_car"
        // 7 elements (06-06-2022)
        tag("amenity") == "boat_rental" -> "directions_boat"
        // 6 elements (06-06-2022)
        tag("amenity") == "vending_machine" -> "storefront"
        // 6 elements (06-06-2022)
        tag("amenity") == "money_transfer" -> "currency_exchange"
        // 6 elements (06-06-2022)
        tag("amenity") == "marketplace" -> "storefront"
        // 6 elements (06-06-2022)
        tag("amenity") == "ice_cream" -> "icecream"
        // 6 elements (06-06-2022)
        tag("amenity") == "coworking_space" -> "business"
        // 6 elements (06-06-2022)
        tag("amenity") == "community_centre" -> "groups"
        // 5 elements (06-06-2022)
        tag("amenity") == "kindergarten" -> "child_care"
        // 5 elements (06-06-2022)
        tag("amenity") == "internet_cafe" -> "public"
        // 4 elements (06-06-2022)
        tag("amenity") == "recycling" -> "delete"
        // 4 elements (06-06-2022)
        tag("amenity") == "payment_centre" -> "currency_exchange"
        // 4 elements (06-06-2022)
        tag("amenity") == "cinema" -> "local_movies"
        // 4 elements (06-06-2022)
        tag("amenity") == "childcare" -> "child_care"
        // 4 elements (06-06-2022)
        tag("amenity") == "bicycle_rental" -> "pedal_bike"
        // 3 elements (06-06-2022)
        tag("amenity") == "townhall" -> "groups"
        // 3 elements (06-06-2022)
        tag("amenity") == "theatre" -> "account_balance"
        // 3 elements (06-06-2022)
        tag("amenity") == "post_office" -> "local_post_office"
        // 3 elements (06-06-2022)
        tag("amenity") == "payment_terminal" -> "currency_exchange"
        // 3 elements (06-06-2022)
        tag("amenity") == "office" -> "business"
        // 3 elements (06-06-2022)
        tag("amenity") == "language_school" -> "school"
        // 3 elements (06-06-2022)
        tag("amenity") == "charging_station" -> "electrical_services"
        // 2 elements (06-06-2022)
        tag("amenity") == "stripclub" -> "adult_content"
        // 2 elements (06-06-2022)
        tag("amenity") == "spa" -> "spa"
        // 1 element (06-06-2022)
        tag("amenity") == "training" -> "school"
        // 1 element (06-06-2022)
        tag("amenity") == "flight_school" -> "flight_takeoff"
        tag("amenity") == "motorcycle_rental" -> "two_wheeler"

        // 490 elements (07-06-2022)
        tag("office") == "company" -> "business"
        // 222 elements (07-06-2022)
        tag("office") == "it" -> "computer"
        // 131 elements (07-06-2022)
        tag("office") == "lawyer" -> "balance"
        // 40 elements (07-06-2022)
        tag("office") == "accountant" -> "attach_money"
        // 30 elements (07-06-2022)
        tag("office") == "architect" -> "architecture"
        // 23 elements (07-06-2022)
        tag("office") == "educational_institution" -> "school"
        // 20 elements (07-06-2022)
        tag("office") == "advertising_agency" -> "business"
        // 17 elements (07-06-2022)
        tag("office") == "estate_agent" -> "home"
        // 13 elements (07-06-2022)
        tag("office") == "therapist" -> "medical_services"
        // 13 elements (07-06-2022)
        tag("office") == "coworking" -> "groups"
        // 12 elements (07-06-2022)
        tag("office") == "physician" -> "medical_services"
        // 11 elements (07-06-2022)
        tag("office") == "marketing" -> "business"
        // 11 elements (07-06-2022)
        tag("office") == "surveyor" -> "business"
        // 10 elements (07-06-2022)
        tag("office") == "financial" -> "attach_money"
        // 10 elements (07-06-2022)
        tag("office") == "association" -> "groups"
        // 9 elements (07-06-2022)
        tag("office") == "engineer" -> "engineering"
        // 8 elements (07-06-2022)
        tag("office") == "telecommunication" -> "cell_tower"
        // 8 elements (07-06-2022)
        tag("office") == "coworking_space" -> "groups"
        // 8 elements (07-06-2022)
        tag("office") == "construction" -> "engineering"
        // 7 elements (07-06-2022)
        tag("office") == "tax_advisor" -> "attach_money"
        // 7 elements (07-06-2022)
        tag("office") == "construction_company" -> "engineering"
        // 6 elements (07-06-2022)
        tag("office") == "travel_agent" -> "tour"
        // 6 elements (07-06-2022)
        tag("office") == "insurance" -> "business"
        // 5 elements (07-06-2022)
        tag("office") == "ngo" -> "business"
        // 5 elements (07-06-2022)
        tag("office") == "newspaper" -> "newspaper"
        // 4 elements (07-06-2022)
        tag("office") == "trade" -> "business"
        // 4 elements (07-06-2022)
        tag("office") == "private" -> "business"
        // 4 elements (07-06-2022)
        tag("office") == "guide" -> "tour"
        // 4 elements (07-06-2022)
        tag("office") == "foundation" -> "business"
        // 3 elements (07-06-2022)
        tag("office") == "web_design" -> "design_services"
        // 2 elements (07-06-2022)
        tag("office") == "graphic_design" -> "design_services"
        // 1 element (07-06-2022)
        tag("office") == "limousine_service" -> "local_taxi"
        tag("office") != null -> "business"

        // 46 elements (08-06-2022)
        tag("leisure") == "sports_centre" -> "fitness_center"
        // 14 elements (08-06-2022)
        tag("leisure") == "hackerspace" -> "computer"
        // 12 elements (08-06-2022)
        tag("leisure") == "fitness_centre" -> "fitness_center"
        // 9 elements (08-06-2022)
        tag("leisure") == "pitch" -> "sports"
        // 4 elements (08-06-2022)
        tag("leisure") == "resort" -> "beach_access"
        // 4 elements (08-06-2022)
        tag("leisure") == "park" -> "park"
        // 4 elements (08-06-2022)
        tag("leisure") == "beach_resort" -> "beach_access"
        // 2 elements (08-06-2022)
        tag("leisure") == "marina" -> "directions_boat"
        // 2 elements (08-06-2022)
        tag("leisure") == "golf_course" -> "golf_course"
        // 2 elements (08-06-2022)
        tag("leisure") == "garden" -> "local_florist"
        // 2 elements (08-06-2022)
        tag("leisure") == "escape_game" -> "games"
        // 2 elements (08-06-2022)
        tag("leisure") == "dance" -> "nightlife"
        // 1 element (08-06-2022)
        tag("leisure") == "kayak_dock" -> "kayaking"
        tag("leisure") == "water_park" -> "pool"

        // 45 elements (18-06-2022)
        tag("healthcare") == "dentist" -> "medical_services"
        // 27 elements (18-06-2022)
        tag("healthcare") == "doctor" -> "medical_services"
        // 18 elements (18-06-2022)
        tag("healthcare") == "clinic" -> "medical_services"
        // 16 elements (18-06-2022)
        tag("healthcare") == "pharmacy" -> "local_pharmacy"
        // 12 elements (18-06-2022)
        tag("healthcare") == "pharmacy" -> "local_pharmacy"
        // 1 element (18-06-2022)
        tag("healthcare") == "optometrist" -> "visibility"
        tag("healthcare") != null -> "medical_services"

        // 112 elements (18-06-2022)
        tag("building") == "commercial" -> "business"
        // 66 elements (18-06-2022)
        tag("building") == "office" -> "business"
        // 44 elements (18-06-2022)
        tag("building") == "retail" -> "storefront"
        // 30 elements (18-06-2022)
        tag("building") == "church" -> "church"

        tag("sport") == "scuba_diving" -> "scuba_diving"

        tag("craft") == "blacksmith" -> "hardware"
        tag("craft") == "photographer" -> "photo_camera"
        tag("craft") == "hvac" -> "hvac"
        tag("craft") == "signmaker" -> "hardware"
        tag("craft") == "brewery" -> "sports_bar"

        tag("company") == "transport" -> "directions_car"

        tag("cuisine") == "burger" -> "lunch_dining"
        tag("cuisine") == "pizza" -> "local_pizza"

        tag("telecom") == "data_center" -> "dns"

        tag("place") == "farm" -> "agriculture"

        else -> ""
    }
}