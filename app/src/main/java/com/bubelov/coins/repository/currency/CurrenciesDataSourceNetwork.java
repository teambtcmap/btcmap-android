package com.bubelov.coins.repository.currency;

import com.bubelov.coins.api.coins.CoinsApi;
import com.bubelov.coins.model.Currency;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class CurrenciesDataSourceNetwork {
    private CoinsApi api;

    @Inject
    CurrenciesDataSourceNetwork(CoinsApi api) {
        this.api = api;
    }

    public Collection<Currency> getCurrencies() throws IOException {
        return api.getCurrencies().execute().body();
    }
}