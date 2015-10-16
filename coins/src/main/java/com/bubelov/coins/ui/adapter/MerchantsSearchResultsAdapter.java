package com.bubelov.coins.ui.adapter;

import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.DistanceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 10/12/15 2:36 PM
 */

public class MerchantsSearchResultsAdapter extends RecyclerView.Adapter<MerchantsSearchResultsAdapter.ResultViewHolder> {
    private List<Merchant> merchants = new ArrayList<>();

    private OnMerchantSelectedListener listener;

    private Location userLocation;

    public MerchantsSearchResultsAdapter(OnMerchantSelectedListener listener, Location userLocation) {
        this.listener = listener;
        this.userLocation = userLocation;
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_merchant_search_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ResultViewHolder holder, int position) {
        Merchant merchant = merchants.get(position);
        holder.name.setText(merchant.getName());
        holder.itemView.setOnClickListener(v -> listener.onMerchantSelected(merchant));

        if (userLocation == null) {
            holder.distance.setVisibility(View.GONE);
        } else {
            holder.distance.setVisibility(View.VISIBLE);
            float distanceInMeters = DistanceUtils.getDistance(merchant.getPosition(), userLocation);
            holder.distance.setText(String.format("%.02f km", distanceInMeters / 1000.0f));
        }
    }

    @Override
    public int getItemCount() {
        return merchants.size();
    }

    public List<Merchant> getMerchants() {
        return merchants;
    }

    public static class ResultViewHolder extends RecyclerView.ViewHolder {
        private TextView name;

        private TextView distance;

        public ResultViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            distance = (TextView) itemView.findViewById(R.id.distance);
        }
    }

    public interface OnMerchantSelectedListener {
        void onMerchantSelected(Merchant merchant);
    }
}
