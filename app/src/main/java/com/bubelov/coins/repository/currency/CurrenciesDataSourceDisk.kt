package com.bubelov.coins.repository.currency

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import com.bubelov.coins.model.Currency
import com.bubelov.coins.database.DbContract

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class CurrenciesDataSourceDisk @Inject
internal constructor(private val db: SQLiteDatabase) {
    fun getCurrency(code: String): Currency? {
        db.query(DbContract.Currencies.TABLE_NAME, null,
                "${DbContract.Currencies.CODE} = ?",
                arrayOf(code), null, null, null, null).use { cursor -> return fromCursor(cursor) }
    }

    fun insert(currencies: Collection<Currency>) {
        db.beginTransaction()

        try {
            for (currency in currencies) {
                db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME, null,
                        toContentValues(currency),
                        SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun fromCursor(cursor: Cursor): Currency? {
        if (cursor.moveToNext()) {
            return Currency(
                    id = cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)),
                    name = cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)),
                    code = cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)),
                    crypto = cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1
            )
        } else {
            return null
        }
    }

    private fun toContentValues(currency: Currency): ContentValues {
        val values = ContentValues()
        values.put(DbContract.Currencies._ID, currency.id)
        values.put(DbContract.Currencies.NAME, currency.name)
        values.put(DbContract.Currencies.CODE, currency.code)
        values.put(DbContract.Currencies.CRYPTO, currency.crypto)
        return values
    }
}