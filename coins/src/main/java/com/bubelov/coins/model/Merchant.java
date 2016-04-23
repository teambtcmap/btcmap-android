package com.bubelov.coins.model;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Collection;
import java.util.List;

import timber.log.Timber;

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

    public static long getCount() {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        return DatabaseUtils.queryNumEntries(db, DbContract.Merchants.TABLE_NAME);
    }

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

    // Database stuff

    public static void insert(List<Merchant> merchants) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        db.beginTransaction();

        try {
            insertMerchants(merchants);
            insertCurrenciesMerchants(merchants);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Failed to insert merchants");
        } finally {
            db.endTransaction();
        }
    }

    private static void insertMerchants(List<Merchant> merchants) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

        String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                DbContract.Merchants.TABLE_NAME,
                DbContract.Merchants._ID,
                DbContract.Merchants._CREATED_AT,
                DbContract.Merchants._UPDATED_AT,
                DbContract.Merchants.LATITUDE,
                DbContract.Merchants.LONGITUDE,
                DbContract.Merchants.NAME,
                DbContract.Merchants.DESCRIPTION,
                DbContract.Merchants.PHONE,
                DbContract.Merchants.WEBSITE,
                DbContract.Merchants.AMENITY,
                DbContract.Merchants.OPENING_HOURS,
                DbContract.Merchants.ADDRESS);

        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Merchant merchant : merchants) {
            insertStatement.bindLong(1, merchant.getId());
            insertStatement.bindLong(2, merchant.getCreatedAt().getMillis());
            insertStatement.bindLong(3, merchant.getUpdatedAt().getMillis());
            insertStatement.bindDouble(4, merchant.getLatitude());
            insertStatement.bindDouble(5, merchant.getLongitude());
            insertStatement.bindString(6, getEmptyStringIfNull(merchant.getName()));
            insertStatement.bindString(7, getEmptyStringIfNull(merchant.getDescription()));
            insertStatement.bindString(8, getEmptyStringIfNull(merchant.getPhone()));
            insertStatement.bindString(9, getEmptyStringIfNull(merchant.getWebsite()));
            insertStatement.bindString(10, getEmptyStringIfNull(merchant.getAmenity()));
            insertStatement.bindString(11, getEmptyStringIfNull(merchant.getOpeningHours()));
            insertStatement.bindString(12, getEmptyStringIfNull(merchant.getAddress()));
            insertStatement.execute();
        }
    }

    private static void insertCurrenciesMerchants(List<Merchant> merchants) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        String insertQuery = "insert or replace into currencies_merchants (currency_id, merchant_id) values (?, ?)";
        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Merchant merchant : merchants) {
            for (Currency currency : merchant.getCurrencies()) {
                insertStatement.bindLong(1, currency.getId());
                insertStatement.bindLong(2, merchant.getId());
                insertStatement.execute();
            }
        }
    }

    private static String getEmptyStringIfNull(String string) {
        return string == null ? "" : string;
    }
}