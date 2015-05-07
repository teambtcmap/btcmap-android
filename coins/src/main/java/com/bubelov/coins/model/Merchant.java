package com.bubelov.coins.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Author: Igor Bubelov
 * Date: 02/06/14 21:42
 */

public class Merchant extends AbstractEntity implements ClusterItem {
    private String name;

    private String description;

    private double latitude;

    private double longitude;

    private String amenity;

    private String phone;

    private String website;

    private transient LatLng position;

    @Override
    public LatLng getPosition() {
        if (position == null) {
            position = new LatLng(latitude, longitude);
        }

        return position;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Merchant && ((Merchant) o).getId() == getId();
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

    public String getAmenity() {
        return amenity;
    }

    public void setAmenity(String amenity) {
        this.amenity = amenity;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}