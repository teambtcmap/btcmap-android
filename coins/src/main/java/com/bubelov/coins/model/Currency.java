package com.bubelov.coins.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;

import java.io.Serializable;
import java.util.List;

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

    // Database stuff

    public static void insert(List<Currency> currencies) throws Exception {
        for (Currency currency : currencies) {
            currency.insert();
        }
    }

    public boolean insert() {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        long rowId = db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME, null, toValues(), SQLiteDatabase.CONFLICT_REPLACE);

        if (rowId == -1) {
            return false;
        } else {
            setId(rowId);
            return true;
        }
    }

    private ContentValues toValues() {
        ContentValues values = new ContentValues();

        if (getId() > 0) {
            values.put(DbContract.Currencies._ID, getId());
        }

        values.put(DbContract.Currencies._UPDATED_AT, getUpdatedAt().getMillis());
        values.put(DbContract.Currencies.NAME, getName());
        values.put(DbContract.Currencies.CODE, getCode());
        values.put(DbContract.Currencies.CRYPTO, isCrypto());

        return values;
    }
}