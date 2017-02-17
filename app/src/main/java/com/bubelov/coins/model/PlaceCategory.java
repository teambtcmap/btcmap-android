package com.bubelov.coins.model;

import com.bubelov.coins.R;

/**
 * @author Igor Bubelov
 */

public enum PlaceCategory {
    ATM,
    RESTAURANT,
    CAFE,
    BAR, // TODO merge pub
    HOTEL,
    PIZZA,
    FAST_FOOD,
    HOSPITAL, // TODO merge dentist, doctors, hospital, clinic
    PHARMACY,
    TAXI,
    FUEL;

    public int getIconId() {
        switch (this) {
            case ATM:
                return R.drawable.ic_atm_24dp;
            case RESTAURANT:
                return R.drawable.ic_restaurant_white_24dp;
            case CAFE:
                return R.drawable.ic_cafe_24dp;
            case BAR:
                return R.drawable.ic_bar_24dp;
            case HOTEL:
                return R.drawable.ic_hotel_24dp;
            case PIZZA:
                return R.drawable.ic_pizza_24dp;
            case FAST_FOOD:
                return R.drawable.ic_fast_food;
            case HOSPITAL:
                return R.drawable.ic_hospital_24dp;
            case PHARMACY:
                return R.drawable.ic_pharmacy_24dp;
            case TAXI:
                return R.drawable.ic_taxi_24dp;
            case FUEL:
                return R.drawable.ic_gas_station_24dp;
            default:
                return R.drawable.ic_place_24dp;
        }
    }

    public int getMarkerIconId() {
        switch (this) {
            case ATM:
                return R.drawable.ic_place_atm;
            case RESTAURANT:
                return R.drawable.ic_place_restaurant;
            case CAFE:
                return R.drawable.ic_place_cafe;
            case BAR:
                return R.drawable.ic_place_bar;
            case HOTEL:
                return R.drawable.ic_place_hotel;
            case PIZZA:
                return R.drawable.ic_place_pizza;
            case FAST_FOOD:
                return R.drawable.ic_place_empty;
            case HOSPITAL:
                return R.drawable.ic_place_hospital;
            case PHARMACY:
                return R.drawable.ic_place_pharmacy;
            case TAXI:
                return R.drawable.ic_place_taxi;
            case FUEL:
                return R.drawable.ic_place_fuel;
            default:
                return R.drawable.ic_place_empty;
        }
    }

    public int getPluralStringId() {
        switch (this) {
            case ATM:
                return R.string.atms;
            case RESTAURANT:
                return R.string.restaurants;
            case CAFE:
                return R.string.cafes;
            case BAR:
                return R.string.bars;
            case HOTEL:
                return R.string.hotels;
            case PIZZA:
                return R.string.pizza;
            case FAST_FOOD:
                return R.string.fast_food;
            case HOSPITAL:
                return R.string.hospitals;
            case PHARMACY:
                return R.string.pharmacies;
            case TAXI:
                return R.string.taxi;
            case FUEL:
                return R.string.gas_stations;
            default:
                return android.R.string.ok;
        }
    }
}
