package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.service.ExchangeRatesService;
import com.bubelov.coins.service.ExchangeRatesSource;

import java.text.DateFormat;
import java.text.DecimalFormat;

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

    private TextView price;

    private TextView priceLastCheck;

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
                    itemSelectedListener.onMenuItemSelected(id, item.getText());
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

        price = (TextView) findViewById(R.id.price);
        priceLastCheck = (TextView) findViewById(R.id.price_last_check);

        findViewById(R.id.check).setOnClickListener(v -> updateExchangeRates(true));

        for (int id : ITEMS) {
            findViewById(id).setOnClickListener(v -> setSelected(v.getId()));
        }

        showPrice();

        getContext().getContentResolver().registerContentObserver(Database.Currencies.CONTENT_URI, true, new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                showPrice();
            }
        });

        updateExchangeRates(true);
    }

    private void showPrice() {
        Cursor priceCursor = getContext().getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[] { Database.Currencies.PRICE, Database.Currencies.PRICE_LAST_CHECK },
                String.format("%s = ?", Database.Currencies.CODE),
                new String[] { "BTC" },
                null);

        if (priceCursor.moveToNext()) {
            if (priceCursor.isNull(0)) {
                price.setText("Loading...");
                priceLastCheck.setText("Newer");
                updateExchangeRates(false);
            } else {
                DecimalFormat format = new DecimalFormat();
                format.setMinimumFractionDigits(2);
                format.setMaximumFractionDigits(2);

                price.setText("$" + format.format(priceCursor.getDouble(0)));
                priceLastCheck.setText("Checked at: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(priceCursor.getLong(1)));
            }
        }

        priceCursor.close();
    }

    private void updateExchangeRates(boolean forceLoad) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        ExchangeRatesSource source = ExchangeRatesSource.valueOf(preferences.getString(getContext().getString(R.string.pref_exchange_rates_source_key), null));
        getContext().startService(ExchangeRatesService.newIntent(getContext(), source, forceLoad));
    }

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int id, String title);
    }
}