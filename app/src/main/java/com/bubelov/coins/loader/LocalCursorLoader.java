package com.bubelov.coins.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

public class LocalCursorLoader extends AsyncTaskLoader<Cursor> {
    private final ForceLoadContentObserver observer;

    private SQLiteDatabase database;
    private String table;
    private Uri uri;
    private String[] projection;
    private String selection;
    private String[] selectionArgs;
    private String sortOrder;

    private Cursor cursor;
    private CancellationSignal cancellationSignal;

    public LocalCursorLoader(Context context, SQLiteDatabase database, String table, Uri uri, String[] projection, String selection,
                             String[] selectionArgs, String sortOrder) {
        super(context);
        observer = new ForceLoadContentObserver();
        this.database = database;
        this.table = table;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    @Override
    public Cursor loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }

            cancellationSignal = new CancellationSignal();
        }

        try {
            Cursor cursor = database.query(false, table, projection, selection, selectionArgs, null,
                    null, sortOrder, null, cancellationSignal);

            try {
                // Ensure the cursor window is filled.
                cursor.getCount();
                cursor.registerContentObserver(observer);

                if (uri != null) {
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                }
            } catch (RuntimeException exception) {
                cursor.close();
                throw exception;
            }

            return cursor;
        } finally {
            synchronized (this) {
                cancellationSignal = null;
            }
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }

            return;
        }

        Cursor oldCursor = this.cursor;
        this.cursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous loading. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (cursor != null) {
            deliverResult(cursor);
        }

        if (takeContentChanged() || cursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = null;
    }
}