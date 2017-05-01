package com.bubelov.coins.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.service.DatabaseSyncService;
import com.google.firebase.crash.FirebaseCrash;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import timber.log.Timber;

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
        for (String statement : getSqlStatements("sql/create_tables.sql")) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String statement : getSqlStatements("sql/drop_tables.sql")) {
            db.execSQL(statement);
        }

        onCreate(db);

        DatabaseSyncService.start(context);
    }

    private Collection<String> getSqlStatements(String assetFileName) {
        Collection<String> statements = new ArrayList<>();

        for (String statement : TextUtils.split(getAssetFileContents(assetFileName), ";")) {
            if (!TextUtils.isEmpty(statement)) {
                statements.add(statement);
            }
        }

        return statements;
    }

    private String getAssetFileContents(String fileName) {
        try {
            StringBuilder builder = new StringBuilder();
            InputStream input = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            String buffer;

            while ((buffer = reader.readLine()) != null) {
                builder.append(buffer);
            }

            reader.close();
            return builder.toString();
        } catch (Exception e) {
            Timber.e(e, "Couldn't read asset file");
            FirebaseCrash.report(e);
            return "";
        }
    }
}