package com.bubelov.coins.repository.currency

import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.model.Currency

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class CurrenciesDataSourceApi @Inject
internal constructor(private val api: CoinsApi) {
    @Throws(IOException::class)
    fun getCurrencies(): List<Currency> = api.getCurrencies().execute().body()
}