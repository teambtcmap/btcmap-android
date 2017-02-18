package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.service.NotificationsController;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Igor Bubelov
 */

@Module
public class NotificationsModule {
    @Provides
    @Singleton
    NotificationsController provideNotificationsController(Context context) {
        return new NotificationsController(context);
    }
}
