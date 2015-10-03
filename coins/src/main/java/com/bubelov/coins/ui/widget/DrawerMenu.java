package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.service.ExchangeRatesService;

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

        findViewById(R.id.check).setOnClickListener(v -> updateExchangeRate(true));

        for (int id : ITEMS) {
            findViewById(id).setOnClickListener(v -> setSelected(v.getId()));
        }

        showExchangeRate();

        getContext().getContentResolver().registerContentObserver(Database.Currencies.CONTENT_URI, true, new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                showExchangeRate();
            }
        });

        getContext().getContentResolver().registerContentObserver(Database.ExchangeRates.CONTENT_URI, true, new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                showExchangeRate();
            }
        });

        updateExchangeRate(true);
    }

    private void showExchangeRate() {
        Cursor ratesCursor = getContext().getContentResolver().query(Database.ExchangeRates.CONTENT_URI,
                new String[]{Database.ExchangeRates.VALUE, Database.ExchangeRates._UPDATED_AT},
                String.format("%s = ? and %s = ?", Database.ExchangeRates.SOURCE_CURRENCY_ID, Database.ExchangeRates.TARGET_CURRENCY_ID),
                new String[]{String.valueOf(getCurrencyId("BTC")), String.valueOf(getCurrencyId("USD"))},
                String.format("%s DESC", Database.ExchangeRates._UPDATED_AT));

        if (ratesCursor.moveToNext()) {
            DecimalFormat format = new DecimalFormat();
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);

            price.setText("$" + format.format(ratesCursor.getFloat(0)));
            priceLastCheck.setText(getResources().getString(R.string.bitcoin_price_checked_at) + DateFormat.getTimeInstance(DateFormat.SHORT).format(ratesCursor.getLong(1)));
        } else {
            updateExchangeRate(false);
        }

        ratesCursor.close();
    }

    private void updateExchangeRate(boolean forceLoad) {
        getContext().startService(ExchangeRatesService.newIntent(getContext(), "BTC", "USD", forceLoad));
    }

    private long getCurrencyId(String code) {
        Cursor cursor = getContext().getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID},
                String.format("%s = ?", Database.Currencies.CODE),
                new String[]{code},
                null);

        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int id, String title);
    }
}