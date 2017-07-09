package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.WinkDexApi
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
class Winkdex @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "WinkDex"

    val api: WinkDexApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://winkdex.com/api/v0/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WinkDexApi::class.java)

    override fun getSupportedCurrencyPairs(): Collection<Pair<String, String>> {
        return setOf(Pair("USD", "BTC"))
    }

    override fun getExchangeRate(baseCurrency: String, targetCurrency: String): ExchangeRate {
        if (baseCurrency == "USD" && targetCurrency == "BTC") {
            val rate = api.getPrice().execute().body()!!.price.toDouble() / 100.0f

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