package com.bubelov.coins.model;

import com.bubelov.coins.R;

/**
 * Author: Igor Bubelov
 * Date: 21/05/15 12:40
 */

public enum Amenity {
    ATM,
    CAFE,
    RESTAURANT,
    BAR,
    HOTEL,
    CAR_WASH,
    FUEL,
    HOSPITAL,
    DRY_CLEANING,
    CINEMA,
    PARKING,
    PHARMACY,
    PIZZA,
    TAXI;

    public int getIconId() {
        switch (this) {
            case ATM:
                return R.drawable.ic_atm_24dp;
            case CAFE:
                return R.drawable.ic_cafe_24dp;
            case RESTAURANT:
                return R.drawable.ic_restaurant_24dp;
            case BAR:
                return R.drawable.ic_bar_24dp;
            case HOTEL:
                return R.drawable.ic_hotel_24dp;
            case CAR_WASH:
                return R.drawable.ic_car_wash_24dp;
            case FUEL:
                return R.drawable.ic_gas_station_24dp;
            case HOSPITAL:
                return R.drawable.ic_hospital_24dp;
            case DRY_CLEANING:
                return R.drawable.ic_laundry_24dp;
            case CINEMA:
                return R.drawable.ic_cinema_24dp;
            case PARKING:
                return R.drawable.ic_parking_24dp;
            case PHARMACY:
                return R.drawable.ic_pharmacy_24dp;
            case PIZZA:
                return R.drawable.ic_pizza_24dp;
            case TAXI:
                return R.drawable.ic_taxi_24dp;
            default:
                return R.drawable.ic_place_24dp;
        }
    }

    public int getMarkerIconId() {
        switch (this) {
            case ATM:
                return R.drawable.ic_place_atm;
            case CAFE:
                return R.drawable.ic_place_cafe;
            case RESTAURANT:
                return R.drawable.ic_place_restaurant;
            case BAR:
                return R.drawable.ic_place_bar;
            case HOTEL:
                return R.drawable.ic_place_hotel;
            case CAR_WASH:
                return R.drawable.ic_place_car_wash;
            case FUEL:
                return R.drawable.ic_place_fuel;
            case HOSPITAL:
                return R.drawable.ic_place_hospital;
            case DRY_CLEANING:
                return R.drawable.ic_place_dry_cleaning;
            case CINEMA:
                return R.drawable.ic_place_cinema;
            case PARKING:
                return R.drawable.ic_place_parking;
            case PHARMACY:
                return R.drawable.ic_place_pharmacy;
            case PIZZA:
                return R.drawable.ic_place_pizza;
            case TAXI:
                return R.drawable.ic_place_taxi;
            default:
                return R.drawable.ic_place_empty;
        }
    }
}
