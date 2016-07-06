package com.bubelov.coins.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Currency;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class CurrenciesAdapter extends RecyclerView.Adapter<CurrenciesAdapter.CurrencyViewHolder> {
    private List<Currency> items;

    private CurrenciesAdapterListener listener;

    public CurrenciesAdapter(List<Currency> items, CurrenciesAdapterListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public CurrencyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_currency, parent, false);
        return new CurrencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CurrencyViewHolder holder, int position) {
        final Currency item = items.get(position);
        holder.name.setText(item.getName());

        holder.showOnMap.setOnCheckedChangeListener(null);
        holder.showOnMap.setChecked(item.isShowOnMap());

        holder.showOnMap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setShowOnMap(isChecked);
                listener.onMapVisibilityChanged(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CurrencyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        TextView name;

        @BindView(R.id.show_on_map)
        CheckBox showOnMap;

        public CurrencyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface CurrenciesAdapterListener {
        void onMapVisibilityChanged(Currency currency);
    }
}
