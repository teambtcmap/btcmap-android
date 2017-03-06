package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Igor Bubelov
 */

public class AssetDbHelper extends SQLiteOpenHelper {
    private final Context context;

    private final int version;

    private final String internalDbFilePath;

    private final String assetsDbFilePath;

    private SQLiteDatabase db = null;

    public AssetDbHelper(Context context) {
        super(context, context.getResources().getString(R.string.database_name), null, context.getResources().getInteger(R.integer.database_version));

        this.context = context;
        this.version = context.getResources().getInteger(R.integer.database_version);

        assetsDbFilePath = String.format("databases/%s", getDatabaseName());
        internalDbFilePath = String.format("%s/databases/%s", context.getApplicationInfo().dataDir, getDatabaseName());
    }

    @Override
    public synchronized void close() {
        if (db != null && db.isOpen()) {
            db.close();
            db = null;
        }
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (db != null && db.isOpen() && !db.isReadOnly()) {
            return db;
        }

        boolean success = false;
        SQLiteDatabase db = null;

        try {
            db = createOrOpenDatabase(false);

            int version = db.getVersion();

            if (version != 0 && version < this.version) {
                db = createOrOpenDatabase(true);
                db.setVersion(this.version);
                version = db.getVersion();
            }

            if (version != this.version) {
                db.setVersion(this.version);
            }

            onOpen(db);
            success = true;
            return db;
        } finally {
            if (success) {
                if (this.db != null) {
                    try {
                        this.db.close();
                    } catch (Exception e) {
                    }
                }
                this.db = db;
            } else {
                if (db != null) db.close();
            }
        }
    }

    @Override
    public final void onCreate(SQLiteDatabase db) {
        // Nothing to do here
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing to do here
    }

    private SQLiteDatabase createOrOpenDatabase(boolean override) {
        File dbFile = new File(internalDbFilePath);

        if (!dbFile.exists()) {
            new File(context.getApplicationInfo().dataDir + "/databases/").mkdir();
        }

        if (!dbFile.exists() || override) {
            copyDatabaseFromAssets();
        }

        return SQLiteDatabase.openDatabase(internalDbFilePath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private void copyDatabaseFromAssets() {
        try {
            InputStream in = context.getAssets().open(assetsDbFilePath);
            OutputStream out = new FileOutputStream(internalDbFilePath);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.flush();
            out.close();
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}