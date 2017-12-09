package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.CoinbaseApi
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
class Coinbase @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "Coinbase"

    val api: CoinbaseApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://api.coinbase.com/v2/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CoinbaseApi::class.java)

    override fun getCurrencyPairs(): Collection<CurrencyPair> {
        return listOf(CurrencyPair.BTC_USD)
    }

    override fun getExchangeRate(pair: CurrencyPair): Result<Double> {
        if (pair == CurrencyPair.BTC_USD) {
            return try {
                Result.Success(api.getExchangeRates().execute().body()!!.data!!.rates!!["USD"]!!)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            throw IllegalArgumentException()
        }
    }
}