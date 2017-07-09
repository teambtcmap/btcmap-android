package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.BitcoinAverageApi
import com.bubelov.coins.model.ExchangeRate
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class BitcoinAverage @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "BitcoinAverage"

    val api: BitcoinAverageApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://api.bitcoinaverage.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BitcoinAverageApi::class.java)

    override fun getSupportedCurrencyPairs(): Collection<Pair<String, String>> {
        return setOf(Pair("USD", "BTC"))
    }

    override fun getExchangeRate(baseCurrency: String, targetCurrency: String): ExchangeRate {
        if (baseCurrency == "USD" && targetCurrency == "BTC") {
            val rate = api.getUsdTicker().execute().body()!!.last.toDouble()

            return ExchangeRate(
                    id = 0,
                    source = name,
                    baseCurrencyCode = baseCurrency,
                    targetCurrencyCode = targetCurrency,
                    rate = rate,
                    date = System.currentTimeMillis()
            )
        } else {
            throw IllegalArgumentException()
        }
    }
}