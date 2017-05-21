package com.bubelov.coins.repository.currency;

import android.support.annotation.NonNull;

import com.bubelov.coins.model.Currency;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    public Currency getCurrency(@NonNull String code) {
        return diskDataSource.getCurrency(code);
    }

    public void reloadFromApi() throws IOException {
        Collection<Currency> currencies = networkDataSource.getCurrencies();
        diskDataSource.insert(currencies);
    }
}