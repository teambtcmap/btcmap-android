package com.bubelov.coins.ui.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.bubelov.coins.R;
import com.bubelov.coins.data.RatesApi;
import com.bubelov.coins.data.api.rates.model.ExchangeRate;
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesActivity extends AbstractActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.list)
    RecyclerView ratesView;

    private ExchangeRatesAdapter ratesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_rates);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        ratesView.setLayoutManager(new LinearLayoutManager(this));
        ratesView.setHasFixedSize(true);

        ratesAdapter = new ExchangeRatesAdapter();
        ratesView.setAdapter(ratesAdapter);

        new LoadExchangeRatesTask().execute();
    }

    private class LoadExchangeRatesTask extends AsyncTask<Void, ExchangeRate, Void> {
        @Override
        protected void onPreExecute() {
            ratesAdapter.getItems().clear();
            ratesAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            RatesApi api = dependencies().dataManager().ratesApi();

            try {
                publishProgress(api.getBitstampRate());
            } catch (IOException e) {
                FirebaseCrash.report(e);
            }

            try {
                publishProgress(api.getCoinbaseRate());
            } catch (IOException e) {
                FirebaseCrash.report(e);
            }

            try {
                publishProgress(api.getWinkDexRate());
            } catch (IOException e) {
                FirebaseCrash.report(e);
            }

            try {
                publishProgress(api.getBitcoinAverageRate());
            } catch (IOException e) {
                FirebaseCrash.report(e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(ExchangeRate... values) {
            for (ExchangeRate rate : values) {
                ratesAdapter.getItems().add(rate);
                ratesAdapter.notifyDataSetChanged();
            }
        }
    }
}