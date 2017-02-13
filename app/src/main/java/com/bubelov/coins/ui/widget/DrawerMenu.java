package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.EventBus;
import com.bubelov.coins.R;
import com.bubelov.coins.event.ExchangeRateLoadFinishedEvent;
import com.bubelov.coins.event.ExchangeRateLoadStartedEvent;
import com.bubelov.coins.event.DatabaseSyncedEvent;
import com.bubelov.coins.model.PlaceCategory;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.rates.ExchangeRatesService;
import com.bubelov.coins.util.AnimationListenerAdapter;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class DrawerMenu extends FrameLayout {
    @BindView(R.id.exchange_rate)
    TextView exchangeRateView;

    @BindView(R.id.check)
    View checkExchangeRateButton;

    private boolean exchangeRateLoading;

    private static int[] ITEM_IDS = {
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
            R.id.taxi
    };

    private List<Pair<Integer, PlaceCategory>> itemsToAmenities = new ArrayList<>();

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

    public void setAmenity(PlaceCategory category) {
        if (category == null) {
            setSelected(R.id.all);
            return;
        }

        for (Pair<Integer, PlaceCategory> pair : itemsToAmenities) {
            if (pair.second.equals(category)) {
                setSelected(pair.first);
            }
        }
    }

    private void setSelected(int selectedItemId) {
        MenuItem selectedItem = null;

        for (int itemId : ITEM_IDS) {
            MenuItem itemView = (MenuItem) findViewById(itemId);

            if (itemId == selectedItemId) {
                selectedItem = itemView;
                itemView.setSelected(true);
            } else {
                itemView.setSelected(false);
            }
        }

        PlaceCategory selectedCategory = null;

        for (Pair<Integer, PlaceCategory> pair : itemsToAmenities) {
            if (pair.first == selectedItemId) {
                selectedCategory = pair.second;
            }
        }

        listener.onAmenitySelected(selectedCategory, selectedItem.getText());
    }

    public void setItemSelectedListener(OnItemClickListener itemSelectedListener) {
        this.listener = itemSelectedListener;
    }

    private void init() {
        inflate(getContext(), R.layout.widget_drawer_menu, this);
        ButterKnife.bind(this);
        initItemsToAmenities();
        EventBus.getInstance().register(this);

        checkExchangeRateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkExchangeRateButton.setClickable(false);
                DrawerMenu.this.updateExchangeRate(true);
            }
        });

        showLastExchangeRate();
        updateExchangeRate(false);

        for (int id : ITEM_IDS) {
            findViewById(id).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(v.getId());
                }
            });
        }
    }

    private void initItemsToAmenities() {
        itemsToAmenities.add(new Pair<>(R.id.atms, PlaceCategory.ATM));
        itemsToAmenities.add(new Pair<>(R.id.cafes, PlaceCategory.CAFE));
        itemsToAmenities.add(new Pair<>(R.id.restaurants, PlaceCategory.RESTAURANT));
        itemsToAmenities.add(new Pair<>(R.id.bars, PlaceCategory.BAR));
        itemsToAmenities.add(new Pair<>(R.id.hotels, PlaceCategory.HOTEL));
        itemsToAmenities.add(new Pair<>(R.id.car_washes, PlaceCategory.CAR_WASH));
        itemsToAmenities.add(new Pair<>(R.id.gas_stations, PlaceCategory.FUEL));
        itemsToAmenities.add(new Pair<>(R.id.hospitals, PlaceCategory.HOSPITAL));
        itemsToAmenities.add(new Pair<>(R.id.laundry, PlaceCategory.DRY_CLEANING));
        itemsToAmenities.add(new Pair<>(R.id.movies, PlaceCategory.CINEMA));
        itemsToAmenities.add(new Pair<>(R.id.parking, PlaceCategory.PARKING));
        itemsToAmenities.add(new Pair<>(R.id.pharmacies, PlaceCategory.PHARMACY));
        itemsToAmenities.add(new Pair<>(R.id.pizza, PlaceCategory.PIZZA));
        itemsToAmenities.add(new Pair<>(R.id.taxi, PlaceCategory.TAXI));
    }

    private void showLastExchangeRate() {
        ExchangeRate exchangeRate = ExchangeRate.last("BTC", "USD");

        if (exchangeRate != null) {
            DecimalFormat format = new DecimalFormat();
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            exchangeRateView.setText(getResources().getString(R.string.price_in_dollars, format.format(exchangeRate.getValue())));
        }
    }

    private void updateExchangeRate(boolean forceLoad) {
        getContext().startService(ExchangeRatesService.newIntent(getContext(), "BTC", "USD", forceLoad));
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

    @Subscribe
    public void onDatabaseSynced(DatabaseSyncedEvent e) {
        if (TextUtils.isEmpty(exchangeRateView.getText())) {
            updateExchangeRate(false);
        }
    }

    public interface OnItemClickListener {
        void onAmenitySelected(PlaceCategory category, String title);
    }
}