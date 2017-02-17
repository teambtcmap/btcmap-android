package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.loader.LocalCursorLoader;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.ui.adapter.PlacesSearchResultsAdapter;
import com.bubelov.coins.util.DistanceComparator;
import com.bubelov.coins.util.DistanceUnits;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

/**
 * @author Igor Bubelov
 */

public class FindPlaceActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, PlacesSearchResultsAdapter.OnPlaceSelectedListener {
    public static final String USER_LOCATION_EXTRA = "user_location";

    public static final String PLACE_ID_EXTRA = "place_id";

    private static final int PLACES_LOADER = 0;

    private static final String QUERY_KEY = "query";

    private static final int MIN_QUERY_LENGTH = 2;

    @BindView(R.id.query)
    EditText query;

    @BindView(R.id.clear)
    ImageView clear;

    private PlacesSearchResultsAdapter resultsAdapter;

    private Location userLocation;

    public static void startForResult(Activity activity, Location userLocation, int requestCode) {
        Intent intent = new Intent(activity, FindPlaceActivity.class);
        intent.putExtra(USER_LOCATION_EXTRA, userLocation);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_place);
        ButterKnife.bind(this);

        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView resultsView = ButterKnife.findById(this, R.id.results);
        resultsView.setLayoutManager(new LinearLayoutManager(this));
        resultsView.setHasFixedSize(true);

        userLocation = getIntent().getParcelableExtra(USER_LOCATION_EXTRA);

        resultsAdapter = new PlacesSearchResultsAdapter(this, userLocation, getDistanceUnits());
        resultsView.setAdapter(resultsAdapter);

        DrawableCompat.setTint(clear.getDrawable().mutate(), getResources().getColor(R.color.secondary_text_or_icons));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = args == null ? "" : args.getString(QUERY_KEY);

        return new LocalCursorLoader(this,
                Injector.INSTANCE.getAndroidComponent().database(),
                DbContract.Places.TABLE_NAME,
                null,
                null,
                String.format("%s = 1 and (%s like ? or %s like ?)", DbContract.Places.VISIBLE, DbContract.Places.NAME, DbContract.Places.AMENITY),
                new String[]{"%" + query + "%", "%" + query + "%"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<Place> places = new ArrayList<>();

        while (cursor.moveToNext()) {
            places.add(Place.find(cursor.getLong(0)));
        }

        if (userLocation != null) {
            Collections.sort(places, new DistanceComparator(userLocation));
        }

        resultsAdapter.getPlaces().clear();
        resultsAdapter.getPlaces().addAll(places);
        resultsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        resultsAdapter.getPlaces().clear();
        resultsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPlaceSelected(Place place) {
        Intent data = new Intent();
        data.putExtra(PLACE_ID_EXTRA, place.getId());
        setResult(RESULT_OK, data);
        supportFinishAfterTransition();
    }

    @OnTextChanged(R.id.query)
    void onQueryTextChanged(CharSequence query) {
        if (query.length() >= MIN_QUERY_LENGTH) {
            Bundle args = new Bundle();
            args.putString(QUERY_KEY, query.toString());
            getLoaderManager().restartLoader(PLACES_LOADER, args, this);
            Answers.getInstance().logSearch(new SearchEvent().putQuery(query.toString()));
        } else {
            getLoaderManager().destroyLoader(PLACES_LOADER);
        }

        clear.setVisibility(TextUtils.isEmpty(query) ? View.GONE : View.VISIBLE);
    }

    @OnEditorAction(R.id.query)
    boolean onEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            InputMethodManager inputMethodManager = (InputMethodManager) query.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(query.getWindowToken(), 0);
            return true;
        } else {
            return false;
        }
    }

    @OnClick(R.id.clear)
    void onClearSearch() {
        query.setText("");
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