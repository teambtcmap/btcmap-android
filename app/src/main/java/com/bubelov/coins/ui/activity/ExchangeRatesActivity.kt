package com.bubelov.coins.ui.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager

import com.bubelov.coins.R
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter

import com.bubelov.coins.ui.viewmodel.ExchangeRatesViewModel
import kotlinx.android.synthetic.main.activity_exchange_rates.*

/**
 * @author Igor Bubelov
 */

class ExchangeRatesActivity : AbstractActivity() {
    private lateinit var model: ExchangeRatesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_rates)
        model = ViewModelProviders.of(this).get(ExchangeRatesViewModel::class.java)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        ratesView.layoutManager = LinearLayoutManager(this)
        ratesView.setHasFixedSize(true)
        val ratesAdapter = ExchangeRatesAdapter()
        ratesView.adapter = ratesAdapter

        model.rates.observe(this, Observer { rates ->
            if (rates != null) {
                ratesAdapter.items = rates
                ratesAdapter.notifyDataSetChanged()
            }
        })

        model.pair.value = CurrencyPair.BTC_USD
    }
}