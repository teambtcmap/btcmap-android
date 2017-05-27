package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.BitcoinAverageApi
import com.bubelov.coins.api.rates.BitstampApi
import com.bubelov.coins.api.rates.CoinbaseApi
import com.bubelov.coins.api.rates.WinkDexApi
import com.bubelov.coins.model.ExchangeRate
import com.google.gson.Gson

import java.io.IOException
import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@Singleton
class ExchangeRatesRepository @Inject
internal constructor(httpClient: OkHttpClient, gson: Gson) {
    private val bitcoinAverageApi: BitcoinAverageApi

    private val bitstampApi: BitstampApi

    private val coinbaseApi: CoinbaseApi

    private val winkDexApi: WinkDexApi

    init {
        val converterFactory = GsonConverterFactory.create(gson)

        bitcoinAverageApi = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://api.bitcoinaverage.com/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitcoinAverageApi::class.java)

        bitstampApi = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitstampApi::class.java)

        coinbaseApi = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://api.coinbase.com/v2/")
                .addConverterFactory(converterFactory)
                .build()
                .create(CoinbaseApi::class.java)

        winkDexApi = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://winkdex.com/api/v0/")
                .addConverterFactory(converterFactory)
                .build()
                .create(WinkDexApi::class.java)
    }

    val exchangeRates: List<ExchangeRate>
        get() {
            val rates = ArrayList<ExchangeRate>()

            try {
                rates.add(bitcoinAverageRate)
            } catch (e: IOException) {
                Timber.e(e, "Couldn't fetch exchange rate")
            }

            try {
                rates.add(bitstampRate)
            } catch (e: IOException) {
                Timber.e(e, "Couldn't fetch exchange rate")
            }

            try {
                rates.add(coinbaseRate)
            } catch (e: IOException) {
                Timber.e(e, "Couldn't fetch exchange rate")
            }

            try {
                rates.add(winkDexRate)
            } catch (e: IOException) {
                Timber.e(e, "Couldn't fetch exchange rate")
            }

            return rates
        }

    private val bitcoinAverageRate: ExchangeRate
        @Throws(IOException::class)
        get() {
            val rate = bitcoinAverageApi.usdTicker.execute().body().last.toDouble()

            return ExchangeRate(
                    id = 0,
                    source = "BitcoinAverage",
                    baseCurrencyCode = "USD",
                    targetCurrencyCode = "BTC",
                    rate = rate,
                    date = System.currentTimeMillis()
            )
        }

    private val bitstampRate: ExchangeRate
        @Throws(IOException::class)
        get() {
            val rate = bitstampApi.ticker.execute().body().last.toDouble()

            return ExchangeRate(
                    id = 0,
                    source = "Bitstamp",
                    baseCurrencyCode = "USD",
                    targetCurrencyCode = "BTC",
                    rate = rate,
                    date = System.currentTimeMillis()
            )
        }

    private val coinbaseRate: ExchangeRate
        @Throws(IOException::class)
        get() {
            val rate = coinbaseApi.exchangeRates.execute().body().data.rates["USD"]

            return ExchangeRate(
                    id = 0,
                    source = "Coinbase",
                    baseCurrencyCode = "USD",
                    targetCurrencyCode = "BTC",
                    rate = rate ?: 0.toDouble(),
                    date = System.currentTimeMillis()
            )
        }

    private val winkDexRate: ExchangeRate
        @Throws(IOException::class)
        get() {
            val rate = winkDexApi.price.execute().body().price.toDouble() / 100.0f

            return ExchangeRate(
                    id = 0,
                    source = "WinkDex",
                    baseCurrencyCode = "USD",
                    targetCurrencyCode = "BTC",
                    rate = rate,
                    date = System.currentTimeMillis()
            )
        }
}