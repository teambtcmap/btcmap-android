package com.bubelov.coins.server;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;

import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 13:58
 */

public interface ServerFacade {
    Collection<Merchant> getMerchants(Currency currency) throws ServerException;
}
