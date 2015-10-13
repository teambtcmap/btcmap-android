package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.dao.CurrencyDAO;
import com.bubelov.coins.dao.ExchangeRateDao;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.rates.ExchangeRatesService;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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

    private List<Pair<Integer, Amenity>> itemsToAmenities = new ArrayList<>();

    private TextView price;

    private TextView priceLastCheck;

    private Listener listener;

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

    public void setAmenity(Amenity amenity) {
        if (amenity == null) {
            setSelected(R.id.all);
            return;
        }

        for (Pair<Integer, Amenity> pair : itemsToAmenities) {
            if (pair.second.equals(amenity)) {
                setSelected(pair.first);
            }
        }
    }

    private void setSelected(int itemId) {
        MenuItem selectedItem = null;
        boolean justNotify = itemId == R.id.settings || itemId == R.id.help_and_feedback;

        for (int id : ITEMS) {
            MenuItem item = (MenuItem) findViewById(id);

            if (id == itemId) {
                selectedItem = item;

                if (!justNotify) {
                    item.setSelected(true);
                }
            } else {
                if (!justNotify) {
                    item.setSelected(false);
                }
            }
        }

        switch (itemId) {
            case R.id.all:
                listener.onAmenitySelected(null, selectedItem.getText());
                break;
            case R.id.settings:
                listener.onSettingsSelected();
                break;
            case R.id.help_and_feedback:
                listener.onFeedbackSelected();
                break;
            default:
                for (Pair<Integer, Amenity> pair : itemsToAmenities) {
                    if (pair.first == itemId) {
                        listener.onAmenitySelected(pair.second, selectedItem.getText());
                    }
                }
        }
    }

    public void setItemSelectedListener(Listener itemSelectedListener) {
        this.listener = itemSelectedListener;
    }

    private void init() {
        inflate(getContext(), R.layout.widget_drawer_menu, this);

        initItemsToAmenities();

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

    private void initItemsToAmenities() {
        itemsToAmenities.add(new Pair<>(R.id.atms, Amenity.ATM));
        itemsToAmenities.add(new Pair<>(R.id.cafes, Amenity.CAFE));
        itemsToAmenities.add(new Pair<>(R.id.restaurants, Amenity.RESTAURANT));
        itemsToAmenities.add(new Pair<>(R.id.bars, Amenity.BAR));
        itemsToAmenities.add(new Pair<>(R.id.hotels, Amenity.HOTEL));
        itemsToAmenities.add(new Pair<>(R.id.car_washes, Amenity.CAR_WASH));
        itemsToAmenities.add(new Pair<>(R.id.gas_stations, Amenity.FUEL));
        itemsToAmenities.add(new Pair<>(R.id.hospitals, Amenity.HOSPITAL));
        itemsToAmenities.add(new Pair<>(R.id.laundry, Amenity.DRY_CLEANING));
        itemsToAmenities.add(new Pair<>(R.id.movies, Amenity.CINEMA));
        itemsToAmenities.add(new Pair<>(R.id.parking, Amenity.PARKING));
        itemsToAmenities.add(new Pair<>(R.id.pharmacies, Amenity.PHARMACY));
        itemsToAmenities.add(new Pair<>(R.id.pizza, Amenity.PIZZA));
        itemsToAmenities.add(new Pair<>(R.id.taxi, Amenity.TAXI));
    }

    private void showExchangeRate() {
        try {
            ExchangeRate exchangeRate = ExchangeRateDao.queryForLast(getContext(),
                    CurrencyDAO.query(getContext(), "BTC"),
                    CurrencyDAO.query(getContext(), "USD"));

            if (exchangeRate != null) {
                DecimalFormat format = new DecimalFormat();
                format.setMinimumFractionDigits(2);
                format.setMaximumFractionDigits(2);

                price.setText("$" + format.format(exchangeRate.getValue()));
                priceLastCheck.setText(getResources().getString(R.string.bitcoin_price_checked_at) + DateFormat.getTimeInstance(DateFormat.SHORT).format(exchangeRate.getUpdatedAt().getMillis()));
            } else {
                updateExchangeRate(false);
            }
        } catch (Exception ignored) {

        }
    }

    private void updateExchangeRate(boolean forceLoad) {
        getContext().startService(ExchangeRatesService.newIntent(getContext(),
                CurrencyDAO.query(getContext(), "BTC"),
                CurrencyDAO.query(getContext(), "USD"),
                forceLoad));
    }

    public interface Listener {
        void onAmenitySelected(Amenity amenity, String title);

        void onSettingsSelected();

        void onFeedbackSelected();
    }
}