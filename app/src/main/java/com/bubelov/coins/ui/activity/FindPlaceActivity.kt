package com.bubelov.coins.ui.activity

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.adapter.PlacesSearchResultsAdapter
import com.bubelov.coins.ui.viewmodel.FindPlaceViewModel

import kotlinx.android.synthetic.main.activity_find_place.*

/**
 * @author Igor Bubelov
 */

class FindPlaceActivity : AbstractActivity(), PlacesSearchResultsAdapter.Callback {
    private val viewModel = lazy { ViewModelProviders.of(this).get(FindPlaceViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_place)
        viewModel.value.init(intent.getParcelableExtra<Location>(USER_LOCATION_EXTRA))

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        results.layoutManager = LinearLayoutManager(this)
        results.setHasFixedSize(true)

        val resultsAdapter = PlacesSearchResultsAdapter(this, viewModel.value.userLocation, viewModel.value.distanceUnits)
        results.adapter = resultsAdapter

        viewModel.value.searchResults.observe(this, Observer { places ->
            resultsAdapter.places = places
            resultsAdapter.notifyDataSetChanged()
        })

        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.value.searchQuery = s.toString()
                clear.visibility = if (TextUtils.isEmpty(s)) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {
                // Nothing to do here
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

    override fun onPlaceClick(place: Place) {
        val data = Intent()
        data.putExtra(PLACE_ID_EXTRA, place.id())
        setResult(Activity.RESULT_OK, data)
        supportFinishAfterTransition()
    }

    companion object {
        const val USER_LOCATION_EXTRA = "user_location"

        const val PLACE_ID_EXTRA = "place_id"

        fun startForResult(activity: Activity, userLocation: Location, requestCode: Int) {
            val intent = Intent(activity, FindPlaceActivity::class.java)
            intent.putExtra(USER_LOCATION_EXTRA, userLocation)
            activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
        }
    }
}