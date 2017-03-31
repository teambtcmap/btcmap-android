package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.service.DatabaseSyncService;

/**
 * @author Igor Bubelov
 */

public class DbHelper extends SQLiteOpenHelper {
    private Context context;

    public DbHelper(Context context) {
        super(context,
                BuildConfig.DATABASE_NAME,
                null,
                BuildConfig.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String statement : new AssetSqlQuery(context, "create_tables").getStatements()) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String statement : new AssetSqlQuery(context, "drop_tables").getStatements()) {
            db.execSQL(statement);
        }

        DatabaseSyncService.start(context);
    }
}