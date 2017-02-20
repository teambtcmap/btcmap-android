package com.bubelov.coins.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.R;
import com.bubelov.coins.database.AssetDbHelper;
import com.bubelov.coins.service.DatabaseSync;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Igor Bubelov
 */

@Module
public class DatabaseModule {
    @Provides
    @Singleton
    SQLiteDatabase database(Context context) {
        SQLiteOpenHelper helper = new AssetDbHelper(context, context.getResources().getString(R.string.database_name), context.getResources().getInteger(R.integer.database_version));
        return helper.getWritableDatabase();
    }

    @Provides
    @Singleton
    DatabaseSync databaseSync() {
        return new DatabaseSync();
    }
}