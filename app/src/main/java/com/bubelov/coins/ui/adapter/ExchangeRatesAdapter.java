package com.bubelov.coins.ui.adapter;

import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.bubelov.coins.api.rates.provider.CryptoExchange;

import java.util.List;

import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesAdapter extends RecyclerView.Adapter<ExchangeRatesAdapter.ExchangeRateViewHolder> {
    private List<Pair<CryptoExchange, Double>> items;

    public ExchangeRatesAdapter(List<Pair<CryptoExchange, Double>> items) {
        this.items = items;
    }

    @Override
    public ExchangeRateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(final ExchangeRateViewHolder holder, int position) {
        // TODO
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ExchangeRateViewHolder extends RecyclerView.ViewHolder {
        public ExchangeRateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
