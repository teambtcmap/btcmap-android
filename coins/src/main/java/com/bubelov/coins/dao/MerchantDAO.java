package com.bubelov.coins.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;
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
    public static final String[] FULL_PROJECTION = new String[]{Database.Merchants._ID, Database.Merchants.NAME, Database.Merchants.DESCRIPTION, Database.Merchants.LATITUDE, Database.Merchants.LONGITUDE, Database.Merchants.AMENITY, Database.Merchants.PHONE, Database.Merchants.WEBSITE, Database.Merchants.OPENING_HOURS, Database.Merchants.ADDRESS, Database.Merchants._CREATED_AT, Database.Merchants._UPDATED_AT};

    public static Merchant query(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(Database.Merchants.CONTENT_URI,
                FULL_PROJECTION,
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

    public static void insertMerchants(Context context, List<Merchant> merchants) {
        ContentValues[] merchantsValues = new ContentValues[merchants.size()];
        List<ContentValues> currenciesMerchantsValues = new ArrayList<>();

        for (int i = 0; i < merchants.size(); i++) {
            Merchant merchant = merchants.get(i);

            ContentValues merchantValues = new ContentValues();
            merchantValues.put(Database.Merchants._ID, merchant.getId());
            merchantValues.put(Database.Merchants._CREATED_AT, merchant.getCreatedAt().getMillis());
            merchantValues.put(Database.Merchants._UPDATED_AT, merchant.getUpdatedAt().getMillis());
            merchantValues.put(Database.Merchants.LATITUDE, merchant.getLatitude());
            merchantValues.put(Database.Merchants.LONGITUDE, merchant.getLongitude());
            merchantValues.put(Database.Merchants.NAME, merchant.getName());
            merchantValues.put(Database.Merchants.DESCRIPTION, merchant.getDescription());
            merchantValues.put(Database.Merchants.PHONE, merchant.getPhone());
            merchantValues.put(Database.Merchants.WEBSITE, merchant.getWebsite());
            merchantValues.put(Database.Merchants.AMENITY, merchant.getAmenity());
            merchantValues.put(Database.Merchants.OPENING_HOURS, merchant.getOpeningHours());
            merchantValues.put(Database.Merchants.ADDRESS, merchant.getAddress());

            merchantsValues[i] = merchantValues;

            for (Currency currency : merchant.getCurrencies()) {
                ContentValues currencyMerchantValues = new ContentValues();
                currencyMerchantValues.put(Database.CurrenciesMerchants.CURRENCY_ID, currency.getId());
                currencyMerchantValues.put(Database.CurrenciesMerchants.MERCHANT_ID, merchant.getId());
                currenciesMerchantsValues.add(currencyMerchantValues);
            }
        }

        context.getContentResolver().bulkInsert(Database.Merchants.CONTENT_URI, merchantsValues);
        context.getContentResolver().bulkInsert(Database.CurrenciesMerchants.CONTENT_URI, currenciesMerchantsValues.toArray(new ContentValues[currenciesMerchantsValues.size()]));
    }
}
