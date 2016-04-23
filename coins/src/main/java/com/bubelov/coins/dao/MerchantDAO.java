package com.bubelov.coins.dao;

import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Merchant;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/13/15 8:35 PM
 */

public class MerchantDAO {
    public static Merchant query(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(DbContract.Merchants.CONTENT_URI,
                null,
                String.format("%s = ?", DbContract.Merchants._ID),
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
        merchant.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)));
        merchant.setName(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.NAME)));
        merchant.setDescription(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.DESCRIPTION)));
        merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Merchants.LATITUDE)));
        merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Merchants.LONGITUDE)));
        merchant.setAmenity(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.AMENITY)));
        merchant.setPhone(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.PHONE)));
        merchant.setWebsite(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.WEBSITE)));
        merchant.setOpeningHours(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.OPENING_HOURS)));
        merchant.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Merchants._CREATED_AT))));
        merchant.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Merchants._UPDATED_AT))));
        return merchant;
    }
}