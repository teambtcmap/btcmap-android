package com.bubelov.coins.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bubelov.coins.R;
import com.bubelov.coins.model.PlaceCategory;
import com.bubelov.coins.ui.adapter.PlaceCategoriesAdapter;

import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class PlaceCategoryDialog extends DialogFragment implements PlaceCategoriesAdapter.Listener {
    public static final String TAG = PlaceCategoryDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        ViewGroup content = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_place_category, null);

        RecyclerView categoriesView = ButterKnife.findById(content, R.id.categories);
        categoriesView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        PlaceCategoriesAdapter adapter = new PlaceCategoriesAdapter();
        adapter.setListener(this);
        categoriesView.setAdapter(adapter);

        builder.setView(content);
        return builder.create();
    }

    @Override
    public void onPlaceCategorySelected(PlaceCategory category) {
        if (getActivity() instanceof PlaceCategoriesAdapter.Listener) {
            ((PlaceCategoriesAdapter.Listener) getActivity()).onPlaceCategorySelected(category);
        }

        dismiss();
    }
}