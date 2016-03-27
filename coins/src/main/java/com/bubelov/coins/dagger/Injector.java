package com.bubelov.coins.dagger;

import com.bubelov.coins.App;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:59
 */

public enum Injector {
    INSTANCE;

    AppComponent appComponent;

    public void initAppComponent(App app) {
        appComponent = DaggerAppComponent.builder().appContextModule(new AppContextModule(app)).build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}
