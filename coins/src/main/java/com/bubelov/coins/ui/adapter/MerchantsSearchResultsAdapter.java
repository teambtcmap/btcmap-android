package com.bubelov.coins.ui.adapter;

import android.content.Context;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.DistanceUnits;
import com.bubelov.coins.util.DistanceUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 10/12/15 2:36 PM
 */

public class MerchantsSearchResultsAdapter extends RecyclerView.Adapter<MerchantsSearchResultsAdapter.ResultViewHolder> {
    private List<Merchant> merchants = new ArrayList<>();

    private OnMerchantSelectedListener listener;

    private Location userLocation;

    private DistanceUnits distanceUnits;

    public MerchantsSearchResultsAdapter(OnMerchantSelectedListener listener, Location userLocation, DistanceUnits distanceUnits) {
        this.listener = listener;
        this.userLocation = userLocation;
        this.distanceUnits = distanceUnits;
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_merchant_search_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ResultViewHolder holder, int position) {
        final Merchant merchant = merchants.get(position);
        holder.name.setText(merchant.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onMerchantSelected(merchant);
            }
        });

        if (userLocation == null) {
            holder.distance.setVisibility(View.GONE);
        } else {
            holder.distance.setVisibility(View.VISIBLE);

            Context context = holder.name.getContext();
            float distanceInKilometers = DistanceUtils.getDistance(merchant.getPosition(), userLocation) / 1000.0f;

            if (distanceUnits.equals(DistanceUnits.KILOMETERS)) {
                holder.distance.setText(context.getString(R.string.msa_distance_kilometers, distanceInKilometers));
            } else {
                float distanceInMiles = DistanceUtils.toMiles(distanceInKilometers);
                holder.distance.setText(context.getString(R.string.msa_distance_miles, distanceInMiles));
            }
        }

        boolean amenityFound = false;

        for (Amenity amenity : Amenity.values()) {
            if (merchant.getAmenity().equalsIgnoreCase(amenity.name())) {
                amenityFound = true;
                holder.icon.setImageResource(amenity.getIconId());
            }
        }

        if (!amenityFound) {
            holder.icon.setImageResource(R.drawable.ic_place_24dp);
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
        @Bind(R.id.icon) ImageView icon;

        @Bind(R.id.name) TextView name;

        @Bind(R.id.distance) TextView distance;

        public ResultViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnMerchantSelectedListener {
        void onMerchantSelected(Merchant merchant);
    }
}
