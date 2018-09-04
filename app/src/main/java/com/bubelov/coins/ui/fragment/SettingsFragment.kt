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

package com.bubelov.coins.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bubelov.coins.R
import com.bubelov.coins.ui.viewmodel.SettingsViewModel
import com.bubelov.coins.util.viewModelProvider
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

class SettingsFragment : DaggerFragment() {
    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private val model by lazy { viewModelProvider(modelFactory) as SettingsViewModel }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        currencyButton.setOnClickListener { model.showCurrencySelector() }
        model.selectedCurrency.observe(this, Observer { currency.text = it })

        model.currencySelectorItems.observe(this, Observer { items ->
            if (items != null && items.isNotEmpty()) {
                val titles = items.map { it.title }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.currency)
                    .setItems(titles) { _, index ->
                        model.selectCurrency(items[index].currency)
                    }
                    .setOnDismissListener { model.currencySelectorItems.value = null }
                    .show()
            }
        })

        distanceUnitsButton.setOnClickListener { _ ->
            val labels = resources.getStringArray(R.array.distance_units)
            val values = resources.getStringArray(R.array.distance_units_values)

            val selectedUnits = model.distanceUnits.value ?: return@setOnClickListener
            val selectedValueIndex = values.indexOf(selectedUnits)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_distance_units)
                .setSingleChoiceItems(labels, selectedValueIndex) { dialog, index ->
                    model.distanceUnits.setValue(values[index])
                    dialog.dismiss()
                }
                .show()
        }

        model.distanceUnits.observe(this, Observer {
            val labels = resources.getStringArray(R.array.distance_units)
            val values = resources.getStringArray(R.array.distance_units_values)
            distanceUnits.text = labels[values.indexOf(it)]
        })

        syncDatabase.setOnClickListener { model.syncDatabase() }

        showSyncLog.setOnClickListener { model.showSyncLogs() }

        model.syncLogs.observe(this, Observer { logs ->
            if (logs != null && !logs.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setItems(logs.toTypedArray(), null)
                    .setOnDismissListener { model.syncLogs.value = null }
                    .show()
            }
        })

        testNotification.setOnClickListener { model.testNotification() }
    }
}