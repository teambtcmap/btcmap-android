package com.bubelov.coins.dagger;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:59
 */

public enum Injector {
    INSTANCE;

    AppComponent appComponent;

    public AppComponent getAppComponent() {
        if (appComponent == null) {
            appComponent = DaggerAppComponent.create();
        }

        return appComponent;
    }
}
