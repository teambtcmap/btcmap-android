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
class CurrenciesDataSourceDb @Inject
internal constructor(private val db: SQLiteDatabase) {
    fun getCurrency(code: String): Currency? {
        db.query(DbContract.Currencies.TABLE_NAME, null,
                "${DbContract.Currencies.CODE} = ?",
                arrayOf(code), null, null, null, null).use { cursor ->
            return if (cursor.moveToNext()) cursor.toCurrency() else null
        }
    }

    fun insertCurrencies(currencies: Collection<Currency>) {
        db.insert(currencies)
    }

    private fun Cursor.toCurrency() = Currency(
            id = getLong(getColumnIndex(DbContract.Currencies._ID)),
            name = getString(getColumnIndex(DbContract.Currencies.NAME)),
            code = getString(getColumnIndex(DbContract.Currencies.CODE)),
            crypto = getInt(getColumnIndex(DbContract.Currencies.CRYPTO)) == 1
    )

    private fun Currency.toContentValues() = ContentValues().apply {
        put(DbContract.Currencies._ID, id)
        put(DbContract.Currencies.NAME, name)
        put(DbContract.Currencies.CODE, code)
        put(DbContract.Currencies.CRYPTO, crypto)
    }

    private fun SQLiteDatabase.insert(currencies: Collection<Currency>) {
        beginTransaction()

        try {
            for (currency in currencies) {
                insertWithOnConflict(DbContract.Currencies.TABLE_NAME, null,
                        currency.toContentValues(),
                        SQLiteDatabase.CONFLICT_REPLACE)
            }

            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }
}