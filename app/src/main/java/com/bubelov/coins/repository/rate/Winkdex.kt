package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.WinkDexApi
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
class Winkdex @Inject constructor(httpClient: OkHttpClient, gson: Gson) : ExchangeRatesSource {
    override val name = "WinkDex"

    val api: WinkDexApi = Retrofit.Builder()
            .client(httpClient)
            .baseUrl("https://winkdex.com/api/v0/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WinkDexApi::class.java)

    override fun getCurrencyPairs(): Collection<CurrencyPair> {
        return listOf(CurrencyPair.BTC_USD)
    }

    override fun getExchangeRate(pair: CurrencyPair): Result<Double> {
        if (pair == CurrencyPair.BTC_USD) {
            return try {
                Result.Success(api.getPrice().execute().body()!!.price.toDouble() / 100.0f)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            throw IllegalArgumentException()
        }
    }
}