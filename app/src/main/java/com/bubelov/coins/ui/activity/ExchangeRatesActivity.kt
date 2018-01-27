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

package com.bubelov.coins.ui.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager

import com.bubelov.coins.R
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter

import com.bubelov.coins.ui.viewmodel.ExchangeRatesViewModel
import kotlinx.android.synthetic.main.activity_exchange_rates.*
import org.jetbrains.anko.selector

class ExchangeRatesActivity : AppCompatActivity() {
    private lateinit var model: ExchangeRatesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_rates)
        model = ViewModelProviders.of(this).get(ExchangeRatesViewModel::class.java)

        toolbar.apply {
            setNavigationOnClickListener { supportFinishAfterTransition() }
            inflateMenu(R.menu.exchange_rates)

            setOnMenuItemClickListener {
                if (it.itemId == R.id.currency) {
                    selector(title = getString(R.string.currency), items = CurrencyPair.values().map { it.displayCurrency }, onClick = { _, index ->
                        val pair = CurrencyPair.values()[index]
                        model.pair.value = pair
                        model.analytics.logEvent("change_exchange_rates_currency_pair", pair.toString())
                    })
                }

                true
            }
        }

        ratesView.layoutManager = LinearLayoutManager(this)
        ratesView.setHasFixedSize(true)
        val ratesAdapter = ExchangeRatesAdapter()
        ratesView.adapter = ratesAdapter

        model.pair.observe(this, Observer {
            if (it != null) {
                toolbar.menu.findItem(R.id.currency).title = it.displayCurrency
            }
        })

        model.rates.observe(this, Observer { rates ->
            if (rates != null) {
                ratesAdapter.items = rates
                ratesAdapter.notifyDataSetChanged()
            }
        })

        model.pair.value = CurrencyPair.BTC_USD
    }
}