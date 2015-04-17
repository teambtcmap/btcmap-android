package com.bubelov.coins;

import com.bubelov.coins.model.Merchant;

import java.util.List;

import retrofit.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 17/04/15 10:42
 */

public interface Api {
    @GET("/merchants")
    List<Merchant> getMerchants();
}
