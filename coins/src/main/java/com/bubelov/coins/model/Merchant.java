package com.bubelov.coins.model;

import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import org.joda.time.DateTime;

import java.util.Collection;

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

    private String openingHours;

    private String address;

    private Collection<Currency> currencies;

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

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Collection<Currency> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Collection<Currency> currencies) {
        this.currencies = currencies;
    }

    public static final String[] PROJECTION_FULL = new String[]{Database.Merchants._ID, Database.Merchants.NAME, Database.Merchants.DESCRIPTION, Database.Merchants.LATITUDE, Database.Merchants.LONGITUDE, Database.Merchants.AMENITY, Database.Merchants.PHONE, Database.Merchants.WEBSITE, Database.Merchants.OPENING_HOURS, Database.Merchants.ADDRESS, Database.Merchants._CREATED_AT, Database.Merchants._UPDATED_AT};

    public static Merchant query(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(Database.Merchants.CONTENT_URI,
                PROJECTION_FULL,
                String.format("%s = ?", Database.Merchants._ID),
                new String[]{String.valueOf(id)},
                null);

        Merchant merchant = null;

        if (cursor.moveToNext()) {
            merchant = getMerchant(cursor);
        }

        cursor.close();
        return merchant;
    }

    public static Merchant getMerchant(Cursor cursor) {
        Merchant merchant = new Merchant();
        merchant.setId(cursor.getLong(cursor.getColumnIndex(Database.Currencies._ID)));
        merchant.setName(cursor.getString(cursor.getColumnIndex(Database.Merchants.NAME)));
        merchant.setDescription(cursor.getString(cursor.getColumnIndex(Database.Merchants.DESCRIPTION)));
        merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LATITUDE)));
        merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LONGITUDE)));
        merchant.setAmenity(cursor.getString(cursor.getColumnIndex(Database.Merchants.AMENITY)));
        merchant.setPhone(cursor.getString(cursor.getColumnIndex(Database.Merchants.PHONE)));
        merchant.setWebsite(cursor.getString(cursor.getColumnIndex(Database.Merchants.WEBSITE)));
        merchant.setOpeningHours(cursor.getString(cursor.getColumnIndex(Database.Merchants.OPENING_HOURS)));
        merchant.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Merchants._CREATED_AT))));
        merchant.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Merchants._UPDATED_AT))));
        return merchant;
    }
}