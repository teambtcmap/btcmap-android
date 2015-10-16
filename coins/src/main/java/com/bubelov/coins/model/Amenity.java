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
                return R.drawable.ic_atm;
            case CAFE:
                return R.drawable.ic_cafe;
            case RESTAURANT:
                return R.drawable.ic_restaurant;
            case BAR:
                return R.drawable.ic_bar;
            case HOTEL:
                return R.drawable.ic_hotel;
            case CAR_WASH:
                return R.drawable.ic_car_wash;
            case FUEL:
                return R.drawable.ic_gas_station;
            case HOSPITAL:
                return R.drawable.ic_hospital;
            case DRY_CLEANING:
                return R.drawable.ic_laundry;
            case CINEMA:
                return R.drawable.ic_movies;
            case PARKING:
                return R.drawable.ic_parking;
            case PHARMACY:
                return R.drawable.ic_pharmacy;
            case PIZZA:
                return R.drawable.ic_pizza;
            case TAXI:
                return R.drawable.ic_taxi;
            default:
                return R.drawable.ic_place_24dp;
        }
    }
}
