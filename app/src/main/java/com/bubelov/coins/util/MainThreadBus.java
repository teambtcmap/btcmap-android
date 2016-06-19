package com.bubelov.coins.util;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * Author: Igor Bubelov
 * Date: 01/04/15 14:06
 */

public class MainThreadBus extends Bus {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainThreadBus.super.post(event);
                }
            });
        }
    }
}