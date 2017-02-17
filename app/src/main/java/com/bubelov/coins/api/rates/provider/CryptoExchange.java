package com.bubelov.coins.api.rates.provider;

/**
 * @author Igor Bubelov
 */

public interface CryptoExchange {
    double getCurrentRate() throws Exception;
}