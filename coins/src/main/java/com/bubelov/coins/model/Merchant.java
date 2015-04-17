package com.bubelov.coins.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 02/06/14 21:42
 */

public class Merchant implements ClusterItem {
    private long id;

    private String name;

    private String description;

    private double latitude;

    private double longitude;

    private Collection<Currency> currencies;

    // Ignore for serialization
    private LatLng position;

    @Override
    public LatLng getPosition() {
        if (position == null) {
            position = new LatLng(latitude, longitude);
        }

        return position;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Merchant && ((Merchant) o).id == id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<Currency> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Collection<Currency> currencies) {
        this.currencies = currencies;
    }
}