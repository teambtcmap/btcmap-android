package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.ExchangeRate

/**
 * @author Igor Bubelov
 */

class ExchangeRatesViewModel(application: Application) : AndroidViewModel(application) {
    val rates = MutableLiveData<List<ExchangeRate>>()

    init {
        GetRatesTask().execute()
    }

    private inner class GetRatesTask : AsyncTask<Void, Void, List<ExchangeRate>>() {
        override fun doInBackground(vararg ignore: Void): List<ExchangeRate> {
            return Injector.mainComponent.exchangeRatesRepository().exchangeRates
        }

        override fun onPostExecute(places: List<ExchangeRate>) {
            rates.value = places
        }
    }
}