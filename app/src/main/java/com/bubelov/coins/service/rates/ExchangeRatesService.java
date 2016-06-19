package com.bubelov.coins.service.rates;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.bubelov.coins.EventBus;
import com.bubelov.coins.R;
import com.bubelov.coins.event.ExchangeRateLoadFinishedEvent;
import com.bubelov.coins.event.ExchangeRateLoadStartedEvent;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.CoinsIntentService;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderFactory;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderType;
import com.bubelov.coins.util.Utils;
import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:44
 */

public class ExchangeRatesService extends CoinsIntentService {
    private static final String TAG = ExchangeRatesService.class.getSimpleName();

    private static final String BASE_CURRENCY_EXTRA = "base_currency";

    private static final String CURRENCY_EXTRA = "target_currency";

    private static final String FORCE_LOAD_EXTRA = "force_load";

    private static final long CACHE_LIFETIME_IN_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private String currency;

    private String baseCurrency;

    public ExchangeRatesService() {
        super(TAG);
    }

    public static Intent newIntent(Context context, String currency, String baseCurrency, boolean forceLoad) {
        Intent intent = new Intent(context, ExchangeRatesService.class);
        intent.putExtra(CURRENCY_EXTRA, currency);
        intent.putExtra(BASE_CURRENCY_EXTRA, baseCurrency);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !Utils.isOnline(this)) {
            return;
        }

        currency = intent.getStringExtra(CURRENCY_EXTRA);
        baseCurrency = intent.getStringExtra(BASE_CURRENCY_EXTRA);


        if (TextUtils.isEmpty(currency) || TextUtils.isEmpty(baseCurrency)) {
            return;
        }

        if (intent.getBooleanExtra(FORCE_LOAD_EXTRA, false) || !isCacheUpToDate()) {
            EventBus.getInstance().post(new ExchangeRateLoadStartedEvent());
            updateExchangeRate();
            EventBus.getInstance().post(new ExchangeRateLoadFinishedEvent());
        }
    }

    private boolean isCacheUpToDate() {
        ExchangeRate latestRate = ExchangeRate.last(currency, baseCurrency);
        return latestRate != null && latestRate.getUpdatedAt().isAfter(System.currentTimeMillis() - CACHE_LIFETIME_IN_MILLIS);
    }

    void updateExchangeRate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ExchangeRatesProviderType provider = ExchangeRatesProviderType.valueOf(preferences.getString(getString(R.string.pref_exchange_rates_provider_key), null));

        try {
            ExchangeRate exchangeRate = ExchangeRatesProviderFactory
                    .newProvider(provider)
                    .getExchangeRate(currency, baseCurrency);

            exchangeRate.create();
        } catch (Exception e) {
            Timber.e(e, "Can't load the exchange rate");
            Crashlytics.logException(e);
        }
    }
}