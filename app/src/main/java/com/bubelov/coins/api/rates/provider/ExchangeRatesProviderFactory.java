package com.bubelov.coins.api.rates.provider;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:46 PM
 */

public class ExchangeRatesProviderFactory {
    public static ExchangeRatesProvider newProvider(ExchangeRatesProviderType providerType) {
        switch (providerType) {
            case BITSTAMP:
                return new BitstampExchangeRatesProvider();
            case COINBASE:
                return new CoinbaseExchangeRatesProvider();
            case BITCOIN_AVERAGE:
                return new BitcoinAverageExchangeRatesProvider();
            case WINKDEX:
                return new WinkdexExchangeRatesProvider();
            default:
                throw new UnsupportedOperationException("Unknown provider");
        }
    }
}