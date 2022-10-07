package icons

import androidx.annotation.DrawableRes
import db.Element
import element.tags
import kotlinx.serialization.json.*
import org.btcmap.R

@DrawableRes
fun Element.iconResId(): Int? {
    val tag = fun(tag: String): String? {
        return tags()[tag]?.jsonPrimitive?.contentOrNull
    }

    return when {
        // Tag combos are more specific so they have a higher prio
        tag("amenity") == "fast_food" && tag("cuisine") == "ice_cream" -> R.drawable.baseline_icecream_24

        // 174 elements (22-05-2022)
        tag("tourism") == "hotel" -> R.drawable.baseline_hotel_24
        // 130 elements (22-05-2022)
        tag("tourism") == "attraction" -> R.drawable.baseline_tour_24
        // 94 elements (22-05-2022)
        tag("tourism") == "guest_house" -> R.drawable.baseline_hotel_24
        // 87 elements (22-05-2022)
        tag("tourism") == "apartment" -> R.drawable.baseline_hotel_24
        // 40 elements (22-05-2022)
        tag("tourism") == "hostel" -> R.drawable.baseline_hotel_24
        // 25 elements (22-05-2022)
        tag("tourism") == "chalet" -> R.drawable.baseline_chalet_24
        // 12 elements (22-05-2022)
        tag("tourism") == "camp_site" -> R.drawable.baseline_holiday_village_24
        // 7 elements (22-05-2022)
        tag("tourism") == "gallery" -> R.drawable.baseline_palette_24
        // 7 elements (22-05-2022)
        tag("tourism") == "artwork" -> R.drawable.baseline_palette_24
        // 5 elements (22-05-2022)
        tag("tourism") == "information" -> R.drawable.baseline_info_outline_24
        // 5 elements (22-05-2022)
        tag("tourism") == "museum" -> R.drawable.baseline_museum_24
        // 3 elements (22-05-2022)
        tag("tourism") == "motel" -> R.drawable.baseline_hotel_24

        // 179 elements (31-05-2022)
        tag("shop") == "computer" -> R.drawable.baseline_computer_24
        // 124 elements (31-05-2022)
        tag("shop") == "clothes" -> R.drawable.baseline_storefront_24
        // 110 elements (31-05-2022)
        tag("shop") == "jewelry" -> R.drawable.baseline_diamond_24
        // 89 elements (31-05-2022)
        tag("shop") == "hairdresser" -> R.drawable.baseline_content_cut_24
        // 85 elements (31-05-2022)
        tag("shop") == "electronics" -> R.drawable.baseline_computer_24
        // 74 elements (31-05-2022)
        tag("shop") == "supermarket" -> R.drawable.baseline_local_grocery_store_24
        // 46 elements (31-05-2022)
        tag("shop") == "car_repair" -> R.drawable.baseline_car_repair_24
        // 45 elements (31-05-2022)
        tag("shop") == "beauty" -> R.drawable.baseline_spa_24
        // 44 elements (31-05-2022)
        tag("shop") == "books" -> R.drawable.baseline_menu_book_24
        // 43 elements (31-05-2022)
        tag("shop") == "furniture" -> R.drawable.baseline_chair_24
        // 40 elements (31-05-2022)
        tag("shop") == "convenience" -> R.drawable.baseline_local_grocery_store_24
        // 34 elements (31-05-2022)
        tag("shop") == "gift" -> R.drawable.baseline_card_giftcard_24
        // 33 elements (31-05-2022)
        tag("shop") == "travel_agency" -> R.drawable.baseline_luggage_24
        // 33 elements (31-05-2022)
        tag("shop") == "mobile_phone" -> R.drawable.baseline_smartphone_24
        // 28 elements (31-05-2022)
        tag("shop") == "tobacco" -> R.drawable.baseline_smoking_rooms_24
        // 28 elements (31-05-2022)
        tag("shop") == "car" -> R.drawable.baseline_directions_car_24
        // 27 elements (31-05-2022)
        tag("shop") == "bakery" -> R.drawable.baseline_bakery_dining_24
        // 25 elements (31-05-2022)
        tag("shop") == "massage" -> R.drawable.baseline_spa_24
        // 23 elements (31-05-2022)
        tag("shop") == "florist" -> R.drawable.baseline_local_florist_24
        // 23 elements (31-05-2022)
        tag("shop") == "bicycle" -> R.drawable.baseline_pedal_bike_24
        // 22 elements (31-05-2022)
        tag("shop") == "e-cigarette" -> R.drawable.baseline_vaping_rooms_24
        // 21 elements (31-05-2022)
        tag("shop") == "optician" -> R.drawable.baseline_visibility_24
        // 20 elements (31-05-2022)
        tag("shop") == "photo" -> R.drawable.baseline_photo_camera_24
        // 20 elements (31-05-2022)
        tag("shop") == "deli" -> R.drawable.baseline_tapas_24
        // 19 elements (31-05-2022)
        tag("shop") == "sports" -> R.drawable.baseline_sports_24
        // 16 elements (31-05-2022)
        tag("shop") == "farm" -> R.drawable.baseline_storefront_24
        // 16 elements (31-05-2022)
        tag("shop") == "art" -> R.drawable.baseline_palette_24
        // 15 elements (31-05-2022)
        tag("shop") == "music" -> R.drawable.baseline_music_note_24
        // 15 elements (31-05-2022)
        tag("shop") == "hardware" -> R.drawable.baseline_hardware_24
        // 15 elements (31-05-2022)
        tag("shop") == "copyshop" -> R.drawable.baseline_local_printshop_24
        // 14 elements (31-05-2022)
        tag("shop") == "wine" -> R.drawable.baseline_wine_bar_24
        // 14 elements (31-05-2022)
        tag("shop") == "shoes" -> R.drawable.baseline_storefront_24
        // 14 elements (31-05-2022)
        tag("shop") == "alcohol" -> R.drawable.baseline_liquor_24
        // 13 elements (31-05-2022)
        tag("shop") == "toys" -> R.drawable.baseline_toys_24
        // 13 elements (31-05-2022)
        tag("shop") == "greengrocer" -> R.drawable.baseline_storefront_24
        // 13 elements (31-05-2022)
        tag("shop") == "car_parts" -> R.drawable.baseline_directions_car_24
        // 12 elements (31-05-2022)
        tag("shop") == "tatoo" -> R.drawable.baseline_storefront_24
        // 12 elements (31-05-2022)
        tag("shop") == "pawnbroker" -> R.drawable.baseline_attach_money_24
        // 12 elements (31-05-2022)
        tag("shop") == "garden_centre" -> R.drawable.baseline_local_florist_24
        // 12 elements (31-05-2022)
        tag("shop") == "butcher" -> R.drawable.baseline_storefront_24
        // 11 elements (31-05-2022)
        tag("shop") == "variety_store" -> R.drawable.baseline_storefront_24
        // 11 elements (31-05-2022)
        tag("shop") == "printing" -> R.drawable.baseline_local_printshop_24
        // 11 elements (31-05-2022)
        tag("shop") == "laundry" -> R.drawable.baseline_local_laundry_service_24
        // 10 elements (31-05-2022)
        tag("shop") == "kiosk" -> R.drawable.baseline_storefront_24
        // 9 elements (31-05-2022)
        tag("shop") == "pet" -> R.drawable.baseline_pets_24
        // 9 elements (31-05-2022)
        tag("shop") == "cannabis" -> R.drawable.baseline_grass_24
        // 9 elements (31-05-2022)
        tag("shop") == "boutique" -> R.drawable.baseline_storefront_24
        // 8 elements (31-05-2022)
        tag("shop") == "stationery" -> R.drawable.baseline_edit_24
        // 8 elements (31-05-2022)
        tag("shop") == "pastry" -> R.drawable.baseline_bakery_dining_24
        // 8 elements (31-05-2022)
        tag("shop") == "mall" -> R.drawable.baseline_local_mall_24
        // 8 elements (31-05-2022)
        tag("shop") == "hifi" -> R.drawable.baseline_music_note_24
        // 8 elements (31-05-2022)
        tag("shop") == "estate_agent" -> R.drawable.baseline_home_24
        // 8 elements (31-05-2022)
        tag("shop") == "cosmetics" -> R.drawable.baseline_spa_24
        // 8 elements (31-05-2022)
        tag("shop") == "coffee" -> R.drawable.baseline_coffee_24
        // 7 elements (31-05-2022)
        tag("shop") == "erotic" -> R.drawable.baseline_adult_content_24
        // 7 elements (31-05-2022)
        tag("shop") == "confectionery" -> R.drawable.baseline_storefront_24
        // 7 elements (31-05-2022)
        tag("shop") == "beverages" -> R.drawable.baseline_liquor_24
        // 6 elements (31-05-2022)
        tag("shop") == "video_games" -> R.drawable.baseline_games_24
        // 6 elements (31-05-2022)
        tag("shop") == "newsagent" -> R.drawable.baseline_newspaper_24
        // 6 elements (31-05-2022)
        tag("shop") == "interior_decoration" -> R.drawable.baseline_design_services_24
        // 6 elements (31-05-2022)
        tag("shop") == "electrical" -> R.drawable.baseline_electrical_services_24
        // 6 elements (31-05-2022)
        tag("shop") == "doityourself" -> R.drawable.baseline_hardware_24
        // 6 elements (31-05-2022)
        tag("shop") == "antiques" -> R.drawable.baseline_storefront_24
        // 5 elements (31-05-2022)
        tag("shop") == "watches" -> R.drawable.baseline_watch_24
        // 5 elements (31-05-2022)
        tag("shop") == "trade" -> R.drawable.baseline_storefront_24
        // 5 elements (31-05-2022)
        tag("shop") == "tea" -> R.drawable.baseline_emoji_food_beverage_24
        // 5 elements (31-05-2022)
        tag("shop") == "scuba_diving" -> R.drawable.baseline_scuba_diving_24
        // 5 elements (31-05-2022)
        tag("shop") == "musical_instrument" -> R.drawable.baseline_music_note_24
        // 5 elements (31-05-2022)
        tag("shop") == "dairy" -> R.drawable.baseline_storefront_24
        // 5 elements (31-05-2022)
        tag("shop") == "chocolate" -> R.drawable.baseline_storefront_24
        // 5 elements (31-05-2022)
        tag("shop") == "anime" -> R.drawable.baseline_storefront_24
        // 4 elements (31-05-2022)
        tag("shop") == "tyres" -> R.drawable.baseline_trip_origin_24
        // 4 elements (31-05-2022)
        tag("shop") == "second_hand" -> R.drawable.baseline_storefront_24
        // 4 elements (31-05-2022)
        tag("shop") == "perfumery" -> R.drawable.baseline_storefront_24
        // 4 elements (31-05-2022)
        tag("shop") == "nutrition_supplements" -> R.drawable.baseline_storefront_24
        // 4 elements (31-05-2022)
        tag("shop") == "motorcycle" -> R.drawable.baseline_two_wheeler_24
        // 4 elements (31-05-2022)
        tag("shop") == "lottery" -> R.drawable.baseline_storefront_24
        // 4 elements (31-05-2022)
        tag("shop") == "locksmith" -> R.drawable.baseline_lock_24
        // 4 elements (31-05-2022)
        tag("shop") == "games" -> R.drawable.baseline_games_24
        // 4 elements (31-05-2022)
        tag("shop") == "funeral_directors" -> R.drawable.baseline_church_24
        // 4 elements (31-05-2022)
        tag("shop") == "department_store" -> R.drawable.baseline_local_mall_24
        // 4 elements (31-05-2022)
        tag("shop") == "chemist" -> R.drawable.baseline_science_24
        // 3 elements (14-06-2022)
        tag("shop") == "carpet" -> R.drawable.baseline_storefront_24
        // 3 elements (31-05-2022)
        tag("shop") == "water_sports" -> R.drawable.baseline_sports_24
        // 3 elements (31-05-2022)
        tag("shop") == "water" -> R.drawable.baseline_sports_24
        // 3 elements (31-05-2022)
        tag("shop") == "video" -> R.drawable.baseline_videocam_24
        // 3 elements (31-05-2022)
        tag("shop") == "tailor" -> R.drawable.baseline_checkroom_24
        // 3 elements (31-05-2022)
        tag("shop") == "storage_rental" -> R.drawable.baseline_warehouse_24
        // 3 elements (31-05-2022)
        tag("shop") == "storage" -> R.drawable.baseline_warehouse_24
        // 3 elements (31-05-2022)
        tag("shop") == "outdoor" -> R.drawable.baseline_outdoor_grill_24
        // 3 elements (31-05-2022)
        tag("shop") == "houseware" -> R.drawable.baseline_chair_24
        // 3 elements (31-05-2022)
        tag("shop") == "herbalist" -> R.drawable.baseline_local_florist_24
        // 3 elements (31-05-2022)
        tag("shop") == "health_food" -> R.drawable.baseline_local_florist_24
        // 3 elements (31-05-2022)
        tag("shop") == "grocery" -> R.drawable.baseline_local_grocery_store_24
        // 3 elements (31-05-2022)
        tag("shop") == "food" -> R.drawable.baseline_local_grocery_store_24
        // 3 elements (31-05-2022)
        tag("shop") == "curtain" -> R.drawable.baseline_storefront_24
        // 3 elements (31-05-2022)
        tag("shop") == "boat" -> R.drawable.baseline_sailing_24
        // 1 element (31-05-2022)
        tag("shop") == "wholesale" -> R.drawable.baseline_local_grocery_store_24
        // 0 elements (31-05-2022)
        tag("shop") == "surf" -> R.drawable.baseline_surfing_24
        tag("shop") != null -> R.drawable.baseline_storefront_24

        // 674 elements (06-06-2022)
        tag("amenity") == "restaurant" -> R.drawable.baseline_restaurant_24
        // 336 elements (06-06-2022)
        tag("amenity") == "atm" -> R.drawable.baseline_local_atm_24
        // 199 elements (06-06-2022)
        tag("amenity") == "cafe" -> R.drawable.baseline_local_cafe_24
        // 146 elements (06-06-2022)
        tag("amenity") == "bar" -> R.drawable.baseline_local_bar_24
        // 100 elements (06-06-2022)
        tag("amenity") == "bureau_de_change" -> R.drawable.baseline_currency_exchange_24
        // 87 elements (06-06-2022)
        tag("amenity") == "place_of_worship" -> R.drawable.baseline_church_24
        // 77 elements (06-06-2022)
        tag("amenity") == "fast_food" -> R.drawable.baseline_lunch_dining_24
        // 70 elements (06-06-2022)
        tag("amenity") == "bank" -> R.drawable.baseline_account_balance_24
        // 60 elements (06-06-2022)
        tag("amenity") == "dentist" -> R.drawable.baseline_medical_services_24
        // 54 elements (06-06-2022)
        tag("amenity") == "pub" -> R.drawable.baseline_sports_bar_24
        // 41 elements (06-06-2022)
        tag("amenity") == "doctors" -> R.drawable.baseline_medical_services_24
        // 26 elements (06-06-2022)
        tag("amenity") == "pharmacy" -> R.drawable.baseline_local_pharmacy_24
        // 26 elements (06-06-2022)
        tag("amenity") == "clinic" -> R.drawable.baseline_medical_services_24
        // 23 elements (06-06-2022)
        tag("amenity") == "school" -> R.drawable.baseline_school_24
        // 18 elements (06-06-2022)
        tag("amenity") == "taxi" -> R.drawable.baseline_local_taxi_24
        // 16 elements (06-06-2022)
        tag("amenity") == "studio" -> R.drawable.baseline_mic_24
        // 16 elements (06-06-2022)
        tag("amenity") == "fuel" -> R.drawable.baseline_local_gas_station_24
        // 15 elements (06-06-2022)
        tag("amenity") == "car_rental" -> R.drawable.baseline_directions_car_24
        // 11 elements (06-06-2022)
        tag("amenity") == "arts_centre" -> R.drawable.baseline_palette_24
        // 11 elements (06-06-2022)
        tag("amenity") == "police" -> R.drawable.baseline_local_police_24
        // 10 elements (06-06-2022)
        tag("amenity") == "hospital" -> R.drawable.baseline_local_hospital_24
        // 10 elements (06-06-2022)
        tag("amenity") == "brothel" -> R.drawable.baseline_adult_content_24
        // 9 elements (06-06-2022)
        tag("amenity") == "veterinary" -> R.drawable.baseline_pets_24
        // 8 elements (06-06-2022)
        tag("amenity") == "university" -> R.drawable.baseline_school_24
        // 8 elements (06-06-2022)
        tag("amenity") == "college" -> R.drawable.baseline_school_24
        // 8 elements (06-06-2022)
        tag("amenity") == "car_wash" -> R.drawable.baseline_local_car_wash_24
        // 7 elements (06-06-2022)
        tag("amenity") == "nightclub" -> R.drawable.baseline_nightlife_24
        // 7 elements (06-06-2022)
        tag("amenity") == "driving_school" -> R.drawable.baseline_directions_car_24
        // 7 elements (06-06-2022)
        tag("amenity") == "boat_rental" -> R.drawable.baseline_directions_boat_24
        // 6 elements (06-06-2022)
        tag("amenity") == "vending_machine" -> R.drawable.baseline_storefront_24
        // 6 elements (06-06-2022)
        tag("amenity") == "money_transfer" -> R.drawable.baseline_currency_exchange_24
        // 6 elements (06-06-2022)
        tag("amenity") == "marketplace" -> R.drawable.baseline_storefront_24
        // 6 elements (06-06-2022)
        tag("amenity") == "ice_cream" -> R.drawable.baseline_icecream_24
        // 6 elements (06-06-2022)
        tag("amenity") == "coworking_space" -> R.drawable.baseline_business_24
        // 6 elements (06-06-2022)
        tag("amenity") == "community_centre" -> R.drawable.baseline_groups_24
        // 5 elements (06-06-2022)
        tag("amenity") == "kindergarten" -> R.drawable.baseline_child_care_24
        // 5 elements (06-06-2022)
        tag("amenity") == "internet_cafe" -> R.drawable.baseline_public_24
        // 4 elements (06-06-2022)
        tag("amenity") == "recycling" -> R.drawable.baseline_delete_24
        // 4 elements (06-06-2022)
        tag("amenity") == "payment_centre" -> R.drawable.baseline_currency_exchange_24
        // 4 elements (06-06-2022)
        tag("amenity") == "cinema" -> R.drawable.baseline_local_movies_24
        // 4 elements (06-06-2022)
        tag("amenity") == "childcare" -> R.drawable.baseline_child_care_24
        // 4 elements (06-06-2022)
        tag("amenity") == "bicycle_rental" -> R.drawable.baseline_pedal_bike_24
        // 3 elements (06-06-2022)
        tag("amenity") == "townhall" -> R.drawable.baseline_groups_24
        // 3 elements (06-06-2022)
        tag("amenity") == "theatre" -> R.drawable.baseline_account_balance_24
        // 3 elements (06-06-2022)
        tag("amenity") == "post_office" -> R.drawable.baseline_local_post_office_24
        // 3 elements (06-06-2022)
        tag("amenity") == "payment_terminal" -> R.drawable.baseline_currency_exchange_24
        // 3 elements (06-06-2022)
        tag("amenity") == "office" -> R.drawable.baseline_business_24
        // 3 elements (06-06-2022)
        tag("amenity") == "language_school" -> R.drawable.baseline_school_24
        // 3 elements (06-06-2022)
        tag("amenity") == "charging_station" -> R.drawable.baseline_electrical_services_24
        // 2 elements (06-06-2022)
        tag("amenity") == "stripclub" -> R.drawable.baseline_adult_content_24
        // 2 elements (06-06-2022)
        tag("amenity") == "spa" -> R.drawable.baseline_spa_24
        // 1 element (06-06-2022)
        tag("amenity") == "training" -> R.drawable.baseline_school_24
        // 1 element (06-06-2022)
        tag("amenity") == "flight_school" -> R.drawable.baseline_flight_takeoff_24
        tag("amenity") == "motorcycle_rental" -> R.drawable.baseline_two_wheeler_24

        // 490 elements (07-06-2022)
        tag("office") == "company" -> R.drawable.baseline_business_24
        // 222 elements (07-06-2022)
        tag("office") == "it" -> R.drawable.baseline_computer_24
        // 131 elements (07-06-2022)
        tag("office") == "lawyer" -> R.drawable.baseline_balance_24
        // 40 elements (07-06-2022)
        tag("office") == "accountant" -> R.drawable.baseline_attach_money_24
        // 30 elements (07-06-2022)
        tag("office") == "architect" -> R.drawable.baseline_architecture_24
        // 23 elements (07-06-2022)
        tag("office") == "educational_institution" -> R.drawable.baseline_school_24
        // 20 elements (07-06-2022)
        tag("office") == "advertising_agency" -> R.drawable.baseline_business_24
        // 17 elements (07-06-2022)
        tag("office") == "estate_agent" -> R.drawable.baseline_home_24
        // 13 elements (07-06-2022)
        tag("office") == "therapist" -> R.drawable.baseline_medical_services_24
        // 13 elements (07-06-2022)
        tag("office") == "coworking" -> R.drawable.baseline_groups_24
        // 12 elements (07-06-2022)
        tag("office") == "physician" -> R.drawable.baseline_medical_services_24
        // 11 elements (07-06-2022)
        tag("office") == "marketing" -> R.drawable.baseline_business_24
        // 11 elements (07-06-2022)
        tag("office") == "surveyor" -> R.drawable.baseline_business_24
        // 10 elements (07-06-2022)
        tag("office") == "financial" -> R.drawable.baseline_attach_money_24
        // 10 elements (07-06-2022)
        tag("office") == "association" -> R.drawable.baseline_groups_24
        // 9 elements (07-06-2022)
        tag("office") == "engineer" -> R.drawable.baseline_engineering_24
        // 8 elements (07-06-2022)
        tag("office") == "telecommunication" -> R.drawable.baseline_cell_tower_24
        // 8 elements (07-06-2022)
        tag("office") == "coworking_space" -> R.drawable.baseline_groups_24
        // 8 elements (07-06-2022)
        tag("office") == "construction" -> R.drawable.baseline_engineering_24
        // 7 elements (07-06-2022)
        tag("office") == "tax_advisor" -> R.drawable.baseline_attach_money_24
        // 7 elements (07-06-2022)
        tag("office") == "construction_company" -> R.drawable.baseline_engineering_24
        // 6 elements (07-06-2022)
        tag("office") == "travel_agent" -> R.drawable.baseline_tour_24
        // 6 elements (07-06-2022)
        tag("office") == "insurance" -> R.drawable.baseline_business_24
        // 5 elements (07-06-2022)
        tag("office") == "ngo" -> R.drawable.baseline_business_24
        // 5 elements (07-06-2022)
        tag("office") == "newspaper" -> R.drawable.baseline_newspaper_24
        // 4 elements (07-06-2022)
        tag("office") == "trade" -> R.drawable.baseline_business_24
        // 4 elements (07-06-2022)
        tag("office") == "private" -> R.drawable.baseline_business_24
        // 4 elements (07-06-2022)
        tag("office") == "guide" -> R.drawable.baseline_tour_24
        // 4 elements (07-06-2022)
        tag("office") == "foundation" -> R.drawable.baseline_business_24
        // 3 elements (07-06-2022)
        tag("office") == "web_design" -> R.drawable.baseline_design_services_24
        // 2 elements (07-06-2022)
        tag("office") == "graphic_design" -> R.drawable.baseline_design_services_24
        // 1 element (07-06-2022)
        tag("office") == "limousine_service" -> R.drawable.baseline_local_taxi_24
        tag("office") != null -> R.drawable.baseline_business_24

        // 46 elements (08-06-2022)
        tag("leisure") == "sports_centre" -> R.drawable.baseline_fitness_center_24
        // 14 elements (08-06-2022)
        tag("leisure") == "hackerspace" -> R.drawable.baseline_computer_24
        // 12 elements (08-06-2022)
        tag("leisure") == "fitness_centre" -> R.drawable.baseline_fitness_center_24
        // 9 elements (08-06-2022)
        tag("leisure") == "pitch" -> R.drawable.baseline_sports_24
        // 4 elements (08-06-2022)
        tag("leisure") == "resort" -> R.drawable.baseline_beach_access_24
        // 4 elements (08-06-2022)
        tag("leisure") == "park" -> R.drawable.baseline_park_24
        // 4 elements (08-06-2022)
        tag("leisure") == "beach_resort" -> R.drawable.baseline_beach_access_24
        // 2 elements (08-06-2022)
        tag("leisure") == "marina" -> R.drawable.baseline_directions_boat_24
        // 2 elements (08-06-2022)
        tag("leisure") == "golf_course" -> R.drawable.baseline_golf_course_24
        // 2 elements (08-06-2022)
        tag("leisure") == "garden" -> R.drawable.baseline_local_florist_24
        // 2 elements (08-06-2022)
        tag("leisure") == "escape_game" -> R.drawable.baseline_games_24
        // 2 elements (08-06-2022)
        tag("leisure") == "dance" -> R.drawable.baseline_nightlife_24
        // 1 element (08-06-2022)
        tag("leisure") == "kayak_dock" -> R.drawable.baseline_kayaking_24
        tag("leisure") == "water_park" -> R.drawable.baseline_pool_24

        // 45 elements (18-06-2022)
        tag("healthcare") == "dentist" -> R.drawable.baseline_medical_services_24
        // 27 elements (18-06-2022)
        tag("healthcare") == "doctor" -> R.drawable.baseline_medical_services_24
        // 18 elements (18-06-2022)
        tag("healthcare") == "clinic" -> R.drawable.baseline_medical_services_24
        // 16 elements (18-06-2022)
        tag("healthcare") == "pharmacy" -> R.drawable.baseline_local_pharmacy_24
        // 12 elements (18-06-2022)
        tag("healthcare") == "pharmacy" -> R.drawable.baseline_local_pharmacy_24
        // 1 element (18-06-2022)
        tag("healthcare") == "optometrist" -> R.drawable.baseline_visibility_24
        tag("healthcare") != null -> R.drawable.baseline_medical_services_24

        // 112 elements (18-06-2022)
        tag("building") == "commercial" -> R.drawable.baseline_business_24
        // 66 elements (18-06-2022)
        tag("building") == "office" -> R.drawable.baseline_business_24
        // 44 elements (18-06-2022)
        tag("building") == "retail" -> R.drawable.baseline_storefront_24
        // 30 elements (18-06-2022)
        tag("building") == "church" -> R.drawable.baseline_church_24

        tag("sport") == "scuba_diving" -> R.drawable.baseline_scuba_diving_24

        tag("craft") == "blacksmith" -> R.drawable.baseline_hardware_24
        tag("craft") == "photographer" -> R.drawable.baseline_photo_camera_24
        tag("craft") == "hvac" -> R.drawable.baseline_hvac_24
        tag("craft") == "signmaker" -> R.drawable.baseline_hardware_24

        tag("company") == "transport" -> R.drawable.baseline_directions_car_24

        tag("cuisine") == "burger" -> R.drawable.baseline_lunch_dining_24
        tag("cuisine") == "pizza" -> R.drawable.baseline_local_pizza_24

        tag("telecom") == "data_center" -> R.drawable.baseline_dns_24

        tag("place") == "farm" -> R.drawable.baseline_agriculture_24

        else -> null
    }
}