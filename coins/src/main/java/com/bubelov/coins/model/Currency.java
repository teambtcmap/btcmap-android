package com.bubelov.coins.model;

import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Author: Igor Bubelov
 * Date: 03/07/14 22:54
 */

public class Currency extends AbstractEntity implements Serializable {
    private String name;

    private String code;

    private boolean crypto;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isCrypto() {
        return crypto;
    }

    public void setCrypto(boolean crypto) {
        this.crypto = crypto;
    }

    public static Currency query(Context context, String code) {
        Cursor cursor = context.getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID, Database.Currencies.NAME, Database.Currencies.CODE, Database.Currencies.CRYPTO, Database.Currencies._CREATED_AT, Database.Currencies._UPDATED_AT},
                String.format("%s = ?", Database.Currencies.CODE),
                new String[]{code},
                null);

        try {
            if (cursor.moveToNext()) {
                Currency currency = new Currency();
                currency.setId(cursor.getLong(cursor.getColumnIndex(Database.Currencies._ID)));
                currency.setName(cursor.getString(cursor.getColumnIndex(Database.Currencies.NAME)));
                currency.setCode(cursor.getString(cursor.getColumnIndex(Database.Currencies.CODE)));
                currency.setCrypto(cursor.getInt(cursor.getColumnIndex(Database.Currencies.CRYPTO)) == 1);
                currency.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._CREATED_AT))));
                currency.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._UPDATED_AT))));

                return currency;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }
}