package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.CoinbaseApi
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
class Coinbase @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "Coinbase"

    val api: CoinbaseApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://api.coinbase.com/v2/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CoinbaseApi::class.java)

    override fun getSupportedCurrencyPairs(): Collection<Pair<String, String>> {
        return setOf(Pair("USD", "BTC"))
    }

    override fun getExchangeRate(baseCurrency: String, targetCurrency: String): ExchangeRate {
        if (baseCurrency == "USD" && targetCurrency == "BTC") {
            val rate = api.getExchangeRates().execute().body()!!.data!!.rates!!["USD"]

            return ExchangeRate(
                    id = 0,
                    source = name,
                    baseCurrencyCode = baseCurrency,
                    targetCurrencyCode = targetCurrency,
                    rate = rate ?: 0.toDouble(),
                    date = System.currentTimeMillis()
            )
        } else {
            throw IllegalArgumentException()
        }
    }
}