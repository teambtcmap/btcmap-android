package com.bubelov.coins.ui.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.api.rates.provider.BitcoinAverage;
import com.bubelov.coins.api.rates.provider.Bitstamp;
import com.bubelov.coins.api.rates.provider.Coinbase;
import com.bubelov.coins.api.rates.provider.CryptoExchange;
import com.bubelov.coins.api.rates.provider.Winkdex;
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private List<CryptoExchange> exchanges = new ArrayList<>();

    private Map<CryptoExchange, Double> rates = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_rates);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });

        ratesView.setLayoutManager(new LinearLayoutManager(this));
        ratesView.setHasFixedSize(true);

        exchanges.add(new Bitstamp());
        exchanges.add(new Coinbase());
        exchanges.add(new Winkdex());
        exchanges.add(new BitcoinAverage());

        ratesAdapter = new ExchangeRatesAdapter(exchanges, rates);
        ratesView.setAdapter(ratesAdapter);

        new LoadExchangeRatesTask().execute();
    }

    private class LoadExchangeRatesTask extends AsyncTask<Void, Pair<CryptoExchange, Double>, Void> {
        @Override
        protected void onPreExecute() {
            rates.clear();
            ratesAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (CryptoExchange exchange : exchanges) {
                try {
                    publishProgress(new Pair<>(exchange, exchange.getCurrentRate()));
                } catch (Exception e) {
                    // TODO
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Pair<CryptoExchange, Double>... values) {
            for (Pair<CryptoExchange, Double> rate : values) {
                rates.put(rate.first, rate.second);
                ratesAdapter.notifyDataSetChanged();
            }
        }
    }
}