package com.bubelov.coins.model;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class Place extends AbstractEntity implements ClusterItem {
    private String name;

    private String description;

    private double latitude;

    private double longitude;

    private String amenity;

    private String phone;

    private String website;

    private String openingHours;

    private String address;

    private boolean visible;

    private Collection<Currency> currencies;

    private transient LatLng position;

    protected Date updatedAt;

    public static long getCount() {
        SQLiteDatabase db = Injector.INSTANCE.getAndroidComponent().database();
        return DatabaseUtils.queryNumEntries(db, DbContract.Places.TABLE_NAME);
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
        return o != null && o instanceof Place && ((Place) o).getId() == getId();
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Collection<Currency> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Collection<Currency> currencies) {
        this.currencies = currencies;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Database stuff

    public static Place find(long id) {
        SQLiteDatabase db = Injector.INSTANCE.getAndroidComponent().database();
        Cursor cursor = db.query(DbContract.Places.TABLE_NAME, null, "_id = ?", new String[]{String.valueOf(id)}, null, null, null);

        try {
            return cursor.moveToNext() ? fromCursor(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private static Place fromCursor(Cursor cursor) {
        Place place = new Place();
        place.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)));
        place.setName(cursor.getString(cursor.getColumnIndex(DbContract.Places.NAME)));
        place.setDescription(cursor.getString(cursor.getColumnIndex(DbContract.Places.DESCRIPTION)));
        place.setLatitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)));
        place.setLongitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)));
        place.setAmenity(cursor.getString(cursor.getColumnIndex(DbContract.Places.AMENITY)));
        place.setPhone(cursor.getString(cursor.getColumnIndex(DbContract.Places.PHONE)));
        place.setWebsite(cursor.getString(cursor.getColumnIndex(DbContract.Places.WEBSITE)));
        place.setOpeningHours(cursor.getString(cursor.getColumnIndex(DbContract.Places.OPENING_HOURS)));
        place.setAddress(cursor.getString(cursor.getColumnIndex(DbContract.Places.ADDRESS)));
        place.setVisible(cursor.getLong(cursor.getColumnIndex(DbContract.Places.VISIBLE)) == 1);
        place.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(DbContract.Places._UPDATED_AT))));
        return place;
    }

    public static void insert(List<Place> places) {
        SQLiteDatabase db = Injector.INSTANCE.getAndroidComponent().database();
        db.beginTransaction();

        try {
            insertPlaces(places);
            insertCurrenciesPlaces(places);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Failed to insert places");
        } finally {
            db.endTransaction();
        }
    }

    private static void insertPlaces(List<Place> places) {
        SQLiteDatabase db = Injector.INSTANCE.getAndroidComponent().database();

        String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                DbContract.Places.TABLE_NAME,
                DbContract.Places._ID,
                DbContract.Places._UPDATED_AT,
                DbContract.Places.LATITUDE,
                DbContract.Places.LONGITUDE,
                DbContract.Places.NAME,
                DbContract.Places.DESCRIPTION,
                DbContract.Places.PHONE,
                DbContract.Places.WEBSITE,
                DbContract.Places.AMENITY,
                DbContract.Places.OPENING_HOURS,
                DbContract.Places.ADDRESS,
                DbContract.Places.VISIBLE);

        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Place place : places) {
            insertStatement.bindLong(1, place.getId());
            insertStatement.bindLong(2, place.getUpdatedAt().getTime());
            insertStatement.bindDouble(3, place.getLatitude());
            insertStatement.bindDouble(4, place.getLongitude());
            insertStatement.bindString(5, getEmptyStringIfNull(place.getName()));
            insertStatement.bindString(6, getEmptyStringIfNull(place.getDescription()));
            insertStatement.bindString(7, getEmptyStringIfNull(place.getPhone()));
            insertStatement.bindString(8, getEmptyStringIfNull(place.getWebsite()));
            insertStatement.bindString(9, getEmptyStringIfNull(place.getAmenity()));
            insertStatement.bindString(10, getEmptyStringIfNull(place.getOpeningHours()));
            insertStatement.bindString(11, getEmptyStringIfNull(place.getAddress()));
            insertStatement.bindLong(12, place.isVisible() ? 1 : 0);
            insertStatement.execute();
        }
    }

    private static void insertCurrenciesPlaces(List<Place> places) {
        SQLiteDatabase db = Injector.INSTANCE.getAndroidComponent().database();
        String insertQuery = String.format("insert or replace into %s (%s, %s) values (?, ?)", DbContract.CurrenciesPlaces.TABLE_NAME, DbContract.CurrenciesPlaces.CURRENCY_ID, DbContract.CurrenciesPlaces.PLACE_ID);
        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Place place : places) {
            for (Currency currency : place.getCurrencies()) {
                insertStatement.bindLong(1, currency.getId());
                insertStatement.bindLong(2, place.getId());
                insertStatement.execute();
            }
        }
    }

    private static String getEmptyStringIfNull(String string) {
        return string == null ? "" : string;
    }
}