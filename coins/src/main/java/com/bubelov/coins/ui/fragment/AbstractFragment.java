package com.bubelov.coins.ui.fragment;

import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.app.Fragment;

import com.bubelov.coins.App;

/**
 * Author: Igor Bubelov
 * Date: 21/05/15 12:55
 */

public class AbstractFragment extends Fragment {
    protected SQLiteOpenHelper getDatabaseHelper() {
        return App.getInstance().getDatabaseHelper();
    }
}
