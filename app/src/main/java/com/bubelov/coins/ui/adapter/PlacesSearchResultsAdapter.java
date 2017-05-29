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
import com.bubelov.coins.model.Place;
import com.bubelov.coins.util.DistanceUnits;
import com.bubelov.coins.util.DistanceUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class PlacesSearchResultsAdapter extends RecyclerView.Adapter<PlacesSearchResultsAdapter.ResultViewHolder> {
    private List<Place> places = new ArrayList<>();

    private Callback listener;

    private Location userLocation;

    private DistanceUnits distanceUnits;

    public PlacesSearchResultsAdapter(Callback listener, Location userLocation, DistanceUnits distanceUnits) {
        this.listener = listener;
        this.userLocation = userLocation;
        this.distanceUnits = distanceUnits;
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_places_search_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ResultViewHolder holder, int position) {
        final Place place = places.get(position);
        holder.name.setText(place.getName());
        holder.ripple.setOnClickListener(v -> listener.onPlaceClick(place));

        if (userLocation == null) {
            holder.distance.setVisibility(View.GONE);
        } else {
            holder.distance.setVisibility(View.VISIBLE);

            Context context = holder.name.getContext();
            float distanceInKilometers = DistanceUtils.getDistance(new LatLng(place.getLatitude(), place.getLongitude()), userLocation) / 1000.0f;

            if (distanceUnits.equals(DistanceUnits.KILOMETERS)) {
                holder.distance.setText(context.getString(R.string.n_kilometers, distanceInKilometers));
            } else {
                float distanceInMiles = DistanceUtils.toMiles(distanceInKilometers);
                holder.distance.setText(context.getString(R.string.n_miles, distanceInMiles));
            }
        }

        holder.icon.setImageResource(R.drawable.ic_place);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ripple)
        View ripple;

        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.name)
        TextView name;

        @BindView(R.id.distance)
        TextView distance;

        ResultViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface Callback {
        void onPlaceClick(Place place);
    }
}