package com.bubelov.coins;

import com.bubelov.coins.util.MainThreadBus;
import com.squareup.otto.Bus;

/**
 * Author: Igor Bubelov
 * Date: 01/06/16 21:51
 */

public class EventBus {
    private static EventBus instance;

    private Bus bus;

    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }

        return instance;
    }

    private EventBus() {
        bus = new MainThreadBus();
    }

    public void register(Object object) {
        bus.register(object);
    }

    public void unregister(Object object) {
        bus.unregister(object);
    }

    public void post(Object object) {
        bus.post(object);
    }
}