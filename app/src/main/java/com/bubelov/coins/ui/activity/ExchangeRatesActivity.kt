package com.bubelov.coins.ui.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager

import com.bubelov.coins.R
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter

import com.bubelov.coins.ui.viewmodel.ExchangeRatesViewModel
import kotlinx.android.synthetic.main.activity_exchange_rates.*

/**
 * @author Igor Bubelov
 */

class ExchangeRatesActivity : AbstractActivity() {
    val viewModel = lazy { ViewModelProviders.of(this).get(ExchangeRatesViewModel::class.java) }

    var loading: Boolean = false
    set(value) {
        field = value
        state_switcher.displayedChild = if (value) 1 else 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_rates)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        rates_view.layoutManager = LinearLayoutManager(this)
        rates_view.setHasFixedSize(true)

        val ratesAdapter = ExchangeRatesAdapter()

        rates_view.adapter = ratesAdapter

        loading = true

        viewModel.value.rates.observe(this, Observer { rates ->
            loading = false
            ratesAdapter.items = rates!!
            ratesAdapter.notifyDataSetChanged()
        })
    }
}