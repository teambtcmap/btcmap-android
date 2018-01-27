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

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

import com.bubelov.coins.R
import com.bubelov.coins.ui.adapter.PlacesSearchResultsAdapter
import com.bubelov.coins.ui.viewmodel.PlacesSearchViewModel
import com.bubelov.coins.util.TextWatcherAdapter

import kotlinx.android.synthetic.main.activity_places_search.*

class PlacesSearchActivity : AppCompatActivity() {
    private lateinit var model: PlacesSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_places_search)
        model = ViewModelProviders.of(this).get(PlacesSearchViewModel::class.java)
        model.setUp(intent.getParcelableExtra(USER_LOCATION_EXTRA), intent.getStringExtra(CURRENCY_EXTRA))

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        val resultsAdapter = PlacesSearchResultsAdapter {
            setResult(Activity.RESULT_OK, Intent().apply { putExtra(PLACE_ID_EXTRA, it.placeId) })
            supportFinishAfterTransition()
        }

        results.adapter = resultsAdapter
        results.layoutManager = LinearLayoutManager(this)
        results.setHasFixedSize(true)

        model.searchResults.observe(this, Observer { places ->
            resultsAdapter.items = places!!
            resultsAdapter.notifyDataSetChanged()
        })

        query.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model.searchQuery.value = s.toString()
                clear.visibility = if (TextUtils.isEmpty(s)) View.GONE else View.VISIBLE
            }
        })

        query.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(query.windowToken, 0)
                    true
                }
                else -> false
            }
        }

        clear.setOnClickListener { query.setText("") }
    }

    companion object {
        private const val USER_LOCATION_EXTRA = "user_location"

        private const val CURRENCY_EXTRA = "currency"

        const val PLACE_ID_EXTRA = "place_id"

        fun startForResult(activity: Activity, userLocation: Location?, currency: String, requestCode: Int) {
            val intent = Intent(activity, PlacesSearchActivity::class.java).apply {
                putExtra(USER_LOCATION_EXTRA, userLocation)
                putExtra(CURRENCY_EXTRA, currency)
            }

            activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
        }
    }
}