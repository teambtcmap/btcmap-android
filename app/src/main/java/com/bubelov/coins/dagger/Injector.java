package com.bubelov.coins.dagger;

import com.bubelov.coins.App;

/**
 * @author Igor Bubelov
 */

public enum Injector {
    INSTANCE;

    private CoreComponent coreComponent;

    private AndroidComponent androidComponent;

    public void initCoreComponent() {
        coreComponent = DaggerCoreComponent.builder().build();
    }

    public void initAndroidComponent(App app) {
        androidComponent = DaggerAndroidComponent.builder().androidModule(new AndroidModule(app)).build();
    }

    public CoreComponent getCoreComponent() {
        return coreComponent;
    }

    public AndroidComponent getAndroidComponent() {
        return androidComponent;
    }
}