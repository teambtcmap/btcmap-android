package com.bubelov.coins.ui.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.ViewSwitcher;

import com.bubelov.coins.R;
import com.bubelov.coins.data.repository.rate.ExchangeRatesRepository;
import com.bubelov.coins.domain.ExchangeRate;
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter;

import java.util.Collection;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesActivity extends AbstractActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.state_switcher)
    ViewSwitcher stateSwitcher;

    @BindView(R.id.list)
    RecyclerView ratesView;

    @Inject
    ExchangeRatesRepository exchangeRatesRepository;

    private ExchangeRatesAdapter ratesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dependencies().inject(this);
        setContentView(R.layout.activity_exchange_rates);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        ratesView.setLayoutManager(new LinearLayoutManager(this));
        ratesView.setHasFixedSize(true);

        ratesAdapter = new ExchangeRatesAdapter();
        ratesView.setAdapter(ratesAdapter);

        new LoadExchangeRatesTask().execute();
    }

    private void setLoading(boolean loading) {
        stateSwitcher.setDisplayedChild(loading ? 1 : 0);
    }

    private class LoadExchangeRatesTask extends AsyncTask<Void, Void, Collection<ExchangeRate>> {
        @Override
        protected void onPreExecute() {
            setLoading(true);
            ratesAdapter.getItems().clear();
            ratesAdapter.notifyDataSetChanged();
        }

        @Override
        protected Collection<ExchangeRate> doInBackground(Void... voids) {
            return exchangeRatesRepository.getExchangeRates();
        }

        @Override
        protected void onPostExecute(Collection<ExchangeRate> exchangeRates) {
            ratesAdapter.getItems().addAll(exchangeRates);
            ratesAdapter.notifyDataSetChanged();
            setLoading(false);
        }
    }
}