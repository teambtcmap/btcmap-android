package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.widget.EditText;

import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.adapter.MerchantsSearchResultsAdapter;
import com.bubelov.coins.util.TextWatcherAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 10/11/15 4:07 PM
 */

public class MerchantsSearchActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, MerchantsSearchResultsAdapter.OnMerchantSelectedListener {
    public static final String MERCHANT_EXTRA = "merchant";

    private static final int MERCHANTS_LOADER = 0;

    private static final String QUERY_KEY = "query";

    private MerchantsSearchResultsAdapter resultsAdapter;

    public static void startForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, MerchantsSearchActivity.class);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchants_search);

        Toolbar toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        getSupportActionBar().setTitle(null);

        RecyclerView resultsView = (RecyclerView) findViewById(R.id.results);
        resultsView.setLayoutManager(new LinearLayoutManager(this));
        resultsView.setHasFixedSize(true);

        resultsAdapter = new MerchantsSearchResultsAdapter(this);
        resultsView.setAdapter(resultsAdapter);

        ((EditText) findView(R.id.query)).addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = args == null ? "" : args.getString(QUERY_KEY);

        return new CursorLoader(this,
                Database.Merchants.CONTENT_URI,
                Merchant.PROJECTION_FULL,
                String.format("%s like ? or %s like ?", Database.Merchants.NAME, Database.Merchants.AMENITY),
                new String[]{"%" + query + "%", "%" + query + "%"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<Merchant> merchants = new ArrayList<>();

        while (cursor.moveToNext()) {
            merchants.add(Merchant.query(this, cursor.getLong(0)));
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
}