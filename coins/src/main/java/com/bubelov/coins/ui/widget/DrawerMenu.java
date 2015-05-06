package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.bubelov.coins.R;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 18:27
 */

public class DrawerMenu extends FrameLayout {
    private static int[] ITEMS = {
            R.id.all,
            R.id.atms,
            R.id.cafes,
            R.id.restaurants,
            R.id.bars,
            R.id.hotels,
            R.id.car_washes,
            R.id.gas_stations,
            R.id.hospitals,
            R.id.laundry,
            R.id.movies,
            R.id.parking,
            R.id.pharmacies,
            R.id.pizza,
            R.id.taxi,
            R.id.settings,
            R.id.help_and_feedback
    };

    private OnMenuItemSelectedListener itemSelectedListener;

    public DrawerMenu(Context context) {
        super(context);
        init();
    }

    public DrawerMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawerMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setSelected(int itemId) {
        boolean justNotify = itemId == R.id.settings || itemId == R.id.help_and_feedback;

        for (int id : ITEMS) {
            MenuItem item = (MenuItem) findViewById(id);

            if (id == itemId) {
                if (!justNotify) {
                    item.setSelected(true);
                }

                if (itemSelectedListener != null) {
                    itemSelectedListener.onMenuItemSelected(id, item);
                }
            } else {
                if (!justNotify) {
                    item.setSelected(false);
                }
            }
        }
    }

    public void setItemSelectedListener(OnMenuItemSelectedListener itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

    private void init() {
        inflate(getContext(), R.layout.widget_drawer_menu, this);

        for (int id : ITEMS) {
            findViewById(id).setOnClickListener(v -> setSelected(v.getId()));
        }
    }

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int id, MenuItem item);
    }
}