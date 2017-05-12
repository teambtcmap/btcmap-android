package com.bubelov.coins.data.repository.currency;

import com.bubelov.coins.domain.Currency;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class CurrenciesRepository {
    private CurrenciesDataSourceNetwork networkDataSource;

    private CurrenciesDataSourceDisk diskDataSource;

    @Inject
    CurrenciesRepository(CurrenciesDataSourceNetwork networkDataSource, CurrenciesDataSourceDisk diskDataSource) {
        this.networkDataSource = networkDataSource;
        this.diskDataSource = diskDataSource;
    }

    public Currency getCurrency(String code) {
        return diskDataSource.getCurrency(code);
    }

    public boolean reloadFromNetwork() {
        try {
            Collection<Currency> currencies = networkDataSource.getCurrencies();
            diskDataSource.insert(currencies);
            return true;
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't load currencies");
            return false;
        }
    }
}