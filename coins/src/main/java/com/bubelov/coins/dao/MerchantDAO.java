package com.bubelov.coins.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

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

    public static void insertMerchants(Context context, List<Merchant> merchants) {
        ContentValues[] merchantsValues = new ContentValues[merchants.size()];
        List<ContentValues> currenciesMerchantsValues = new ArrayList<>();

        for (int i = 0; i < merchants.size(); i++) {
            Merchant merchant = merchants.get(i);

            ContentValues merchantValues = new ContentValues();
            merchantValues.put(DbContract.Merchants._ID, merchant.getId());
            merchantValues.put(DbContract.Merchants._CREATED_AT, merchant.getCreatedAt().getMillis());
            merchantValues.put(DbContract.Merchants._UPDATED_AT, merchant.getUpdatedAt().getMillis());
            merchantValues.put(DbContract.Merchants.LATITUDE, merchant.getLatitude());
            merchantValues.put(DbContract.Merchants.LONGITUDE, merchant.getLongitude());
            merchantValues.put(DbContract.Merchants.NAME, merchant.getName());
            merchantValues.put(DbContract.Merchants.DESCRIPTION, merchant.getDescription());
            merchantValues.put(DbContract.Merchants.PHONE, merchant.getPhone());
            merchantValues.put(DbContract.Merchants.WEBSITE, merchant.getWebsite());
            merchantValues.put(DbContract.Merchants.AMENITY, merchant.getAmenity());
            merchantValues.put(DbContract.Merchants.OPENING_HOURS, merchant.getOpeningHours());
            merchantValues.put(DbContract.Merchants.ADDRESS, merchant.getAddress());

            merchantsValues[i] = merchantValues;

            for (Currency currency : merchant.getCurrencies()) {
                ContentValues currencyMerchantValues = new ContentValues();
                currencyMerchantValues.put(DbContract.CurrenciesMerchants.CURRENCY_ID, currency.getId());
                currencyMerchantValues.put(DbContract.CurrenciesMerchants.MERCHANT_ID, merchant.getId());
                currenciesMerchantsValues.add(currencyMerchantValues);
            }
        }

        context.getContentResolver().bulkInsert(DbContract.Merchants.CONTENT_URI, merchantsValues);
        context.getContentResolver().bulkInsert(DbContract.CurrenciesMerchants.CONTENT_URI, currenciesMerchantsValues.toArray(new ContentValues[currenciesMerchantsValues.size()]));
    }
}