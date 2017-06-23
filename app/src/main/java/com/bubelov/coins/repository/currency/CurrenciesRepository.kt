package com.bubelov.coins.repository.currency

import com.bubelov.coins.model.Currency

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class CurrenciesRepository @Inject
internal constructor(private val apiDataSource: CurrenciesDataSourceApi, private val dbDataSource: CurrenciesDataSourceDb) {
    fun getCurrency(code: String): Currency? {
        return dbDataSource.getCurrency(code)
    }

    @Throws(IOException::class)
    fun reloadFromApi() {
        dbDataSource.insertCurrencies(apiDataSource.getCurrencies())
    }
}