package com.bubelov.coins.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.PlaceCategory;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class PlaceCategoriesAdapter extends RecyclerView.Adapter<PlaceCategoriesAdapter.PlaceCategoryViewHolder> {
    private Listener listener;

    @Override
    public PlaceCategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_place_category, parent, false);
        return new PlaceCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PlaceCategoryViewHolder holder, int position) {
        if (position == 0) {
            holder.icon.setImageResource(R.drawable.ic_all_24dp);
            holder.name.setText(R.string.all_places);
        } else {
            PlaceCategory category = PlaceCategory.values()[position - 1];
            holder.icon.setImageResource(category.getIconId());
            holder.name.setText(category.getPluralStringId());
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    int position = holder.getAdapterPosition();
                    listener.onPlaceCategorySelected(position == 0 ? null : PlaceCategory.values()[position - 1]);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return 1 + PlaceCategory.values().length;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class PlaceCategoryViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.name)
        TextView name;

        public PlaceCategoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface Listener {
        void onPlaceCategorySelected(PlaceCategory category);
    }
}
