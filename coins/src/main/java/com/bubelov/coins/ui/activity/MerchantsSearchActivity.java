package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.bubelov.coins.R;
import com.bubelov.coins.dao.MerchantDAO;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.adapter.MerchantsSearchResultsAdapter;
import com.bubelov.coins.util.DistanceComparator;
import com.bubelov.coins.util.DistanceUnits;
import com.bubelov.coins.util.TextWatcherAdapter;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 10/11/15 4:07 PM
 */

public class MerchantsSearchActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, MerchantsSearchResultsAdapter.OnMerchantSelectedListener {
    public static final String USER_LOCATION_EXTRA = "user_location";

    public static final String MERCHANT_EXTRA = "merchant";

    private static final int MERCHANTS_LOADER = 0;

    private static final String QUERY_KEY = "query";

    private MerchantsSearchResultsAdapter resultsAdapter;

    private Location userLocation;

    public static void startForResult(Activity activity, Location userLocation, int requestCode) {
        Intent intent = new Intent(activity, MerchantsSearchActivity.class);
        intent.putExtra(USER_LOCATION_EXTRA, userLocation);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchants_search);
        ButterKnife.bind(this);

        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView resultsView = ButterKnife.findById(this, R.id.results);
        resultsView.setLayoutManager(new LinearLayoutManager(this));
        resultsView.setHasFixedSize(true);

        userLocation = getIntent().getParcelableExtra(USER_LOCATION_EXTRA);

        resultsAdapter = new MerchantsSearchResultsAdapter(this, userLocation, getDistanceUnits());
        resultsView.setAdapter(resultsAdapter);

        ((EditText) ButterKnife.findById(this, R.id.query)).addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Merchants search")
                .putContentType("Screens")
                .putContentId("Merchants search"));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = args == null ? "" : args.getString(QUERY_KEY);

        return new CursorLoader(this,
                Database.Merchants.CONTENT_URI,
                null,
                String.format("%s like ? or %s like ?", Database.Merchants.NAME, Database.Merchants.AMENITY),
                new String[]{"%" + query + "%", "%" + query + "%"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<Merchant> merchants = new ArrayList<>();

        while (cursor.moveToNext()) {
            merchants.add(MerchantDAO.query(this, cursor.getLong(0)));
        }

        if (userLocation != null) {
            Collections.sort(merchants, new DistanceComparator(userLocation));
        }

        resultsAdapter.getMerchants().clear();
        resultsAdapter.getMerchants().addAll(merchants);
        resultsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        resultsAdapter.getMerchants().clear();
        resultsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMerchantSelected(Merchant merchant) {
        Intent data = new Intent();
        data.putExtra(MERCHANT_EXTRA, merchant);
        setResult(RESULT_OK, data);
        supportFinishAfterTransition();
    }

    private void search(String query) {
        if (query.length() >= 3) {
            Bundle args = new Bundle();
            args.putString(QUERY_KEY, query);
            getLoaderManager().restartLoader(MERCHANTS_LOADER, args, this);
        } else {
            getLoaderManager().destroyLoader(MERCHANTS_LOADER);
        }
    }

    private DistanceUnits getDistanceUnits() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String distanceUnitsString = sharedPreferences.getString(getString(R.string.pref_distance_units_key), getString(R.string.pref_distance_units_automatic));

        if (distanceUnitsString.equals(getString(R.string.pref_distance_units_automatic))) {
            return DistanceUnits.getDefault();
        } else {
            return DistanceUnits.valueOf(distanceUnitsString);
        }
    }
}