package com.bubelov.coins.dagger;

import com.bubelov.coins.App;

/**
 * @author Igor Bubelov
 */

public enum Injector {
    INSTANCE;

    GeneralComponent generalComponent;

    AndroidComponent androidComponent;

    public void initGeneralComponent() {
        generalComponent = DaggerGeneralComponent.builder().build();
    }

    public void initAndroidComponent(App app) {
        androidComponent = DaggerAndroidComponent.builder().appContextModule(new AppContextModule(app)).build();
    }

    public GeneralComponent getGeneralComponent() {
        return generalComponent;
    }

    public AndroidComponent getAndroidComponent() {
        return androidComponent;
    }
}