/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.repository.rate

import com.bubelov.coins.api.rates.BitstampApi
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

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

    private fun fromTicker(call: Call<BitstampApi.BitstampTicker>): Result<Double> {
        return try {
            Result.Success(call.execute().body()!!.last.toDouble())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}