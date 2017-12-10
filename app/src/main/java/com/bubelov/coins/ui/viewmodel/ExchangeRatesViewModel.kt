package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.*
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import com.bubelov.coins.ui.model.ExchangeRateQuery
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.NumberFormat
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class ExchangeRatesViewModel : ViewModel() {
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
                                is Result.Success -> ExchangeRateQuery.Success(sources[resultIndex].name, rateFormat.format(result.data))
                                is Result.Error -> ExchangeRateQuery.Error(sources[resultIndex].name)
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

    private val rateFormat = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    init {
        Injector.appComponent.inject(this)
    }
}