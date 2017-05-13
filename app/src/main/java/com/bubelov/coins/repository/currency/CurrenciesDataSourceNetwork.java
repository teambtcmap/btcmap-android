package com.bubelov.coins.repository.currency;

import com.bubelov.coins.api.coins.CoinsApi;
import com.bubelov.coins.model.Currency;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

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
        Response<List<Currency>> response = api.getCurrencies().execute();

        if (!response.isSuccessful()) {
            throw new IOException("Couldn't fetch currencies");
        }

        return response.body();
    }
}