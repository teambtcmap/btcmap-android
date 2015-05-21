package com.bubelov.coins.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 12:17
 */

public abstract class SimpleCursorLoader extends AsyncTaskLoader<Cursor> {
    private Cursor mCursor;

    public SimpleCursorLoader(Context context) {
        super(context);
    }

    @Override
    public abstract Cursor loadInBackground();

    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            if (cursor != null) {
                cursor.close();
            }

            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
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

        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = null;
    }
}
