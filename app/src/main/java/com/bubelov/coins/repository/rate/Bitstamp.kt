package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.BitstampApi
import com.bubelov.coins.api.rates.BitstampTicker
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class Bitstamp @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "Bitstamp"

    val api: BitstampApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://www.bitstamp.net/api/v2/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BitstampApi::class.java)

    override fun getCurrencyPairs(): Collection<CurrencyPair> {
        return listOf(CurrencyPair.BTC_USD, CurrencyPair.BTC_EUR)
    }

    override fun getExchangeRate(pair: CurrencyPair): Result<Double> {
        return when (pair) {
            CurrencyPair.BTC_USD -> fromTicker(api.getBtcUsdTicker())
            CurrencyPair.BTC_EUR -> fromTicker(api.getBtcEurTicker())
            else -> throw IllegalArgumentException()
        }
    }

    private fun fromTicker(call: Call<BitstampTicker>): Result<Double> {
        return try {
            Result.Success(call.execute().body()!!.last.toDouble())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}