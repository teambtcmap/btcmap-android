package com.bubelov.coins.dagger;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {ConverterModule.class})
public interface GeneralComponent {
    Gson provideGson();
}