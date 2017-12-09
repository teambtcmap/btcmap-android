package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import com.bubelov.coins.ui.model.ExchangeRateQuery
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class ExchangeRatesViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var repository: ExchangeRatesRepository

    val pair = MutableLiveData<CurrencyPair>()

    val rates: LiveData<List<ExchangeRateQuery>> = Transformations.switchMap(pair) { pair ->
        val result = MutableLiveData<List<ExchangeRateQuery>>()
        val sources = repository.getExchangeRatesSources(pair)
        val rates = arrayOfNulls<Result<Double>>(sources.size)
        result.value = sources.map { ExchangeRateQuery.Loading(it.name) }

        launch {
            val jobs = sources.mapIndexed { sourceIndex, source ->
                async { rates[sourceIndex] = source.getExchangeRate(pair) }.apply {
                    invokeOnCompletion {
                        result.postValue(rates.mapIndexed { resultIndex, result ->
                            when (result) {
                                is Result.Success -> ExchangeRateQuery.ExchangeRate(sources[resultIndex].name, pair.displayCurrency, result.data)
                                is Result.Error -> {
                                    Timber.w(result.e)
                                    ExchangeRateQuery.Error(sources[resultIndex].name)}
                                null -> ExchangeRateQuery.Loading(sources[resultIndex].name)
                            }
                        })
                    }
                }
            }

            jobs.forEach { it.await() }
        }

        result
    }

    init {
        Injector.appComponent.inject(this)
    }
}