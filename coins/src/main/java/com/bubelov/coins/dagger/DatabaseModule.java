package com.bubelov.coins.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.database.DbHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Igor Bubelov
 * Date: 27/03/16 18:23
 */

@Module
public class DatabaseModule {
    @Provides @Singleton
    SQLiteDatabase database(Context context) {
        return new DbHelper(context).getWritableDatabase();
    }
}
