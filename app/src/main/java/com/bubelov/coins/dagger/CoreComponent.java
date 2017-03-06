package com.bubelov.coins.dagger;

import com.bubelov.coins.api.CoinsApi;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {CoreModule.class})
public interface CoreComponent {
    CoinsApi api();

    Gson gson();
}