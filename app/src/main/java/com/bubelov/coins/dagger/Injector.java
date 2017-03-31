package com.bubelov.coins.dagger;

import android.content.Context;

/**
 * @author Igor Bubelov
 */

public enum Injector {
    INSTANCE;

    private MainComponent mainComponent;

    public void initMainComponent(Context context) {
        mainComponent = DaggerMainComponent.builder().mainModule(new MainModule(context)).build();
    }

    public MainComponent mainComponent() {
        return mainComponent;
    }
}