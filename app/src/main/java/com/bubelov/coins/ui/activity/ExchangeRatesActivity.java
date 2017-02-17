package com.bubelov.coins.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.api.rates.provider.CryptoExchange;
import com.bubelov.coins.ui.adapter.ExchangeRatesAdapter;

import java.util.ArrayList;
import java.util.List;

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

    ExchangeRatesAdapter ratesAdapter;

    List<Pair<CryptoExchange, Double>> ratesList = new ArrayList<>();

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

        ratesAdapter = new ExchangeRatesAdapter(ratesList);
        ratesView.setAdapter(ratesAdapter);
    }
}