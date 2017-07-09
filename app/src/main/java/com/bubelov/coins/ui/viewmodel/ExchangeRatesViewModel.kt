package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import com.bubelov.coins.ui.model.ExchangeRateQuery
import org.jetbrains.anko.doAsync
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class ExchangeRatesViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    internal lateinit var repository: ExchangeRatesRepository

    val rates = MutableLiveData<List<ExchangeRateQuery>>()

    init {
        Injector.mainComponent.inject(this)
    }

    fun setCurrencyPair(baseCurrency: String, targetCurrency: String) {
        val sources = repository.getExchangeRatesSources(baseCurrency, targetCurrency)
        rates.value = sources.map { ExchangeRateQuery.Loading(it.name) }

        sources.forEachIndexed { index, source ->
            doAsync {
                try {
                    val rate = source.getExchangeRate(baseCurrency, targetCurrency)
                    setRate(index, ExchangeRateQuery.ExchangeRate(source.name, targetCurrency, rate.rate))
                } catch (e: Exception) {
                    setRate(index, ExchangeRateQuery.Error(source.name))
                }
            }
        }
    }

    private fun setRate(index: Int, rate: ExchangeRateQuery) {
        rates.postValue(mutableListOf<ExchangeRateQuery>().apply {
            addAll(rates.value!!)
            set(index, rate)
        })
    }
}