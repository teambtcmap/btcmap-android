package com.bubelov.coins.util

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.SharedPreferences
import com.bubelov.coins.R
import org.jetbrains.anko.defaultSharedPreferences

/**
 * @author Igor Bubelov
 */

class SelectedCurrencyLiveData(val context: Context) : LiveData<String>(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onActive() {
        updateSelectedCurrency()
        context.defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInactive() {
        context.defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updateSelectedCurrency()
    }

    private fun updateSelectedCurrency() {
        value = context.defaultSharedPreferences.getString(context.getString(R.string.pref_currency_key), DEFAULT_CURRENCY)
    }

    companion object {
        const val DEFAULT_CURRENCY = "BTC"
    }
}