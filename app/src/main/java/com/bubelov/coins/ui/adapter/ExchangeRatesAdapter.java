package com.bubelov.coins.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

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
        holder.exchangeName.setText(exchange.getClass().getSimpleName());
        double price = rates.containsKey(exchange) ? rates.get(exchange) : 0;

        if (price == 0 && holder.priceSwitcher.getDisplayedChild() == 1) {
            holder.priceSwitcher.setDisplayedChild(0);
        }

        if (price > 0 && holder.priceSwitcher.getDisplayedChild() == 0) {
            holder.priceSwitcher.setDisplayedChild(1);
        }

        holder.price.setText(String.format(Locale.US, "$%.2f", price));
    }

    @Override
    public int getItemCount() {
        return exchanges.size();
    }

    public static class ExchangeRateViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.exchange_name)
        TextView exchangeName;

        @BindView(R.id.price_flipper)
        ViewSwitcher priceSwitcher;

        @BindView(R.id.price)
        TextView price;

        public ExchangeRateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
