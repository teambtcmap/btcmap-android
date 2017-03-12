package com.bubelov.coins.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.api.rates.provider.CryptoExchange;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesAdapter extends RecyclerView.Adapter<ExchangeRatesAdapter.ExchangeRateViewHolder> {
    private final List<CryptoExchange> exchanges;

    private final Map<CryptoExchange, Double> rates;

    public ExchangeRatesAdapter(List<CryptoExchange> exchanges, Map<CryptoExchange, Double> rates) {
        this.exchanges = exchanges;
        this.rates = rates;
    }

    @Override
    public ExchangeRateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_exchange_rate, parent, false);
        return new ExchangeRateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ExchangeRateViewHolder holder, int position) {
        CryptoExchange exchange = exchanges.get(position);
        String name = exchange.getClass().getSimpleName();
        holder.firstLetter.setText(name.substring(0, 1));
        holder.exchangeName.setText(name);
        double price = rates.containsKey(exchange) ? rates.get(exchange) : 0;

        if (price == 0) {
            holder.price.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.price.setVisibility(View.VISIBLE);
            holder.price.setText(String.format(Locale.US, "$%.2f", price));
            holder.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return exchanges.size();
    }

    public static class ExchangeRateViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.first_letter)
        TextView firstLetter;

        @BindView(R.id.exchange_name)
        TextView exchangeName;

        @BindView(R.id.price)
        TextView price;

        @BindView(R.id.progress)
        View progressBar;

        public ExchangeRateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
