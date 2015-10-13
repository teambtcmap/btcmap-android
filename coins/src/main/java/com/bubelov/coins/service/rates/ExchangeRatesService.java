package com.bubelov.coins.service.rates;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bubelov.coins.R;
import com.bubelov.coins.dao.ExchangeRateDao;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.CoinsIntentService;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderFactory;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderType;
import com.bubelov.coins.util.Utils;

import java.util.concurrent.TimeUnit;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:44
 */

public class ExchangeRatesService extends CoinsIntentService {
    private static final String TAG = ExchangeRatesService.class.getSimpleName();

    private static final String SOURCE_CURRENCY_EXTRA = "source_currency";

    private static final String TARGET_CURRENCY_EXTRA = "target_currency";

    private static final String FORCE_LOAD_EXTRA = "force_load";

    private static final long CACHE_LIFETIME_IN_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private Currency sourceCurrency;

    private Currency targetCurrency;

    public ExchangeRatesService() {
        super(TAG);
    }

    public static Intent newIntent(Context context, Currency sourceCurrency, Currency targetCurrency, boolean forceLoad) {
        Intent intent = new Intent(context, ExchangeRatesService.class);
        intent.putExtra(SOURCE_CURRENCY_EXTRA, sourceCurrency);
        intent.putExtra(TARGET_CURRENCY_EXTRA, targetCurrency);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Utils.isOnline(this)) {
            return;
        }

        sourceCurrency = (Currency) intent.getSerializableExtra(SOURCE_CURRENCY_EXTRA);
        targetCurrency = (Currency) intent.getSerializableExtra(TARGET_CURRENCY_EXTRA);

        if (sourceCurrency == null || targetCurrency == null) {
            return;
        }

        if (intent.getBooleanExtra(FORCE_LOAD_EXTRA, false) || !isCacheUpToDate()) {
            updateExchangeRate();
        }
    }

    private boolean isCacheUpToDate() {
        ExchangeRate latestRate = ExchangeRateDao.queryForLast(this, sourceCurrency, targetCurrency);
        return latestRate != null && latestRate.getUpdatedAt().isAfter(System.currentTimeMillis() - CACHE_LIFETIME_IN_MILLIS);
    }

    void updateExchangeRate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ExchangeRatesProviderType provider = ExchangeRatesProviderType.valueOf(preferences.getString(getString(R.string.pref_exchange_rates_provider_key), null));

        try {
            ExchangeRate exchangeRate = ExchangeRatesProviderFactory
                    .newProvider(provider)
                    .getExchangeRate(sourceCurrency, targetCurrency);

            ContentValues values = new ContentValues();
            values.put(Database.ExchangeRates.SOURCE_CURRENCY_ID, exchangeRate.getSourceCurrencyId());
            values.put(Database.ExchangeRates.TARGET_CURRENCY_ID, exchangeRate.getTargetCurrencyId());
            values.put(Database.ExchangeRates.VALUE, exchangeRate.getValue());
            values.put(Database.ExchangeRates._CREATED_AT, exchangeRate.getCreatedAt().getMillis());
            values.put(Database.ExchangeRates._UPDATED_AT, exchangeRate.getUpdatedAt().getMillis());

            getContentResolver().insert(Database.ExchangeRates.CONTENT_URI, values);
        } catch (Exception exception) {
            Log.e(TAG, "Couldn't load exchange rate", exception);
        }
    }
}