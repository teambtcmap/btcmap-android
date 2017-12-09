package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.BitcoinAverageApi
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
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
            .baseUrl("https://apiv2.bitcoinaverage.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BitcoinAverageApi::class.java)

    override fun getCurrencyPairs(): Collection<CurrencyPair> {
        return listOf(CurrencyPair.BTC_USD)
    }

    override fun getExchangeRate(pair: CurrencyPair): Result<Double> {
        if (pair == CurrencyPair.BTC_USD) {
            return try {
                Result.Success(api.getUsdTicker().execute().body()!!.getAsJsonObject("BTCUSD").get("last").asDouble)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            throw IllegalArgumentException()
        }
    }
}