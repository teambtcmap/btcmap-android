package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.ui.adapter.CurrenciesAdapter;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class CurrenciesActivity extends AbstractActivity implements CurrenciesAdapter.CurrenciesAdapterListener {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.currencies)
    RecyclerView currenciesView;

    public static void start(Activity activity) {
        Intent intent = new Intent(activity, CurrenciesActivity.class);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currencies);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });

        currenciesView.setLayoutManager(new LinearLayoutManager(this));
        currenciesView.setHasFixedSize(true);
        currenciesView.setAdapter(new CurrenciesAdapter(Currency.find(false), this));
    }

    @Override
    protected void onStop() {
        Injector.INSTANCE.getAppComponent().getPlacesCache().invalidate();
        super.onStop();
    }

    @Override
    public void onMapVisibilityChanged(Currency currency) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        ContentValues values = new ContentValues();
        values.put(DbContract.Currencies.SHOW_ON_MAP, currency.isShowOnMap());
        db.update(DbContract.Currencies.TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(currency.getId())});
    }
}