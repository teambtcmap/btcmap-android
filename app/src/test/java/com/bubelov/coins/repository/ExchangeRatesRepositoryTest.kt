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

package com.bubelov.coins.repository

import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.rate.BitcoinAverage
import com.bubelov.coins.repository.rate.Bitstamp
import com.bubelov.coins.repository.rate.Coinbase
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class ExchangeRatesRepositoryTest {
    @Mock private lateinit var bitcoinAverage: BitcoinAverage
    @Mock private lateinit var bitstamp: Bitstamp
    @Mock private lateinit var coinbase: Coinbase
    private lateinit var repository: ExchangeRatesRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        repository = ExchangeRatesRepository(
            bitcoinAverage,
            bitstamp,
            coinbase
        )
    }

    @Test
    fun filtersSources() {
        `when`(bitcoinAverage.getCurrencyPairs()).thenReturn(listOf(CurrencyPair.BTC_EUR))
        `when`(bitstamp.getCurrencyPairs()).thenReturn(emptyList())
        `when`(coinbase.getCurrencyPairs()).thenReturn(listOf(CurrencyPair.BTC_GBP))

        val btcEurSources = repository.getExchangeRatesSources(CurrencyPair.BTC_EUR)
        Assert.assertEquals(1, btcEurSources.size)
        Assert.assertEquals(bitcoinAverage, btcEurSources.first())
        verify(bitcoinAverage).getCurrencyPairs()
        verify(bitstamp).getCurrencyPairs()
        verify(coinbase).getCurrencyPairs()

        val btcUsdSources = repository.getExchangeRatesSources(CurrencyPair.BTC_USD)
        Assert.assertTrue(btcUsdSources.isEmpty())

        val btcGbpSources = repository.getExchangeRatesSources(CurrencyPair.BTC_GBP)
        Assert.assertEquals(1, btcGbpSources.size)
        Assert.assertEquals(coinbase, btcGbpSources.first())
    }
}