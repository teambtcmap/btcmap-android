package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.event.ExchangeRateLoadFinishedEvent;
import com.bubelov.coins.event.ExchangeRateLoadStartedEvent;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.rates.ExchangeRatesService;
import com.bubelov.coins.util.AnimationListenerAdapter;
import com.squareup.otto.Subscribe;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 18:27
 */

public class DrawerMenu extends FrameLayout {
    private Currency btc;
    private Currency usd;

    @Bind(R.id.exchange_rate) TextView exchangeRate;

    @Bind(R.id.exchange_rate_last_check) TextView exchangeRateLastCheck;

    @Bind(R.id.check) View checkExchangeRateButton;

    private boolean exchangeRateLoading;

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

    private OnItemClickListener listener;

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

    public void setItemSelectedListener(OnItemClickListener itemSelectedListener) {
        this.listener = itemSelectedListener;
    }

    private void init() {
        inflate(getContext(), R.layout.widget_drawer_menu, this);
        ButterKnife.bind(this);
        initItemsToAmenities();
        App.getInstance().getBus().register(this);

        checkExchangeRateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkExchangeRateButton.setClickable(false);
                DrawerMenu.this.updateExchangeRate(true);
            }
        });

        btc = Currency.findByCode("BTC");
        usd = Currency.findByCode("USD");

        showLastExchangeRate();
        updateExchangeRate(false);

        for (int id : ITEMS) {
            findViewById(id).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(v.getId());
                }
            });
        }
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

    private void showLastExchangeRate() {
        ExchangeRate exchangeRate = ExchangeRate.last(btc, usd);

        if (exchangeRate != null) {
            DecimalFormat format = new DecimalFormat();
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);

            this.exchangeRate.setText(getResources().getString(R.string.price_in_dollars, format.format(exchangeRate.getValue())));
            exchangeRateLastCheck.setText(getResources().getString(R.string.bitcoin_price_checked_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(exchangeRate.getUpdatedAt().getMillis())));
        }
    }

    private void updateExchangeRate(boolean forceLoad) {
        getContext().startService(ExchangeRatesService.newIntent(getContext(), btc, usd, forceLoad));
    }

    @Subscribe
    public void onExchangeRateLoadStarted(ExchangeRateLoadStartedEvent event) {
        exchangeRateLoading = true;

        if (checkExchangeRateButton.getAnimation() == null) {
            RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(1000);
            rotateAnimation.setRepeatCount(Animation.INFINITE);
            rotateAnimation.setInterpolator(new LinearInterpolator());
            checkExchangeRateButton.startAnimation(rotateAnimation);

            rotateAnimation.setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationRepeat(Animation animation) {
                    if (!exchangeRateLoading) {
                        checkExchangeRateButton.clearAnimation();
                        checkExchangeRateButton.setClickable(true);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    showLastExchangeRate();
                }
            });
        }
    }

    @Subscribe
    public void onExchangeRateLoadFinished(ExchangeRateLoadFinishedEvent event) {
        exchangeRateLoading = false;
    }

    public interface OnItemClickListener {
        void onAmenitySelected(Amenity amenity, String title);

        void onSettingsSelected();

        void onFeedbackSelected();
    }
}