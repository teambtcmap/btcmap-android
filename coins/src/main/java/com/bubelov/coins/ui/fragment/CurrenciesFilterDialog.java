package com.bubelov.coins.ui.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatDialog;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;

/**
 * Author: Igor Bubelov
 * Date: 01/05/15 21:04
 */

public class CurrenciesFilterDialog extends AppCompatDialog {
    private static final String TAG = CurrenciesFilterDialog.class.getSimpleName();

    public CurrenciesFilterDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(getContext());
        listView.setAdapter(getAdapter());
        setContentView(listView);
    }

    private ListAdapter getAdapter() {
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

        Cursor currencies = db.query(Tables.Currencies.TABLE_NAME,
                new String[]{Tables.Currencies._ID, Tables.Currencies.NAME, Tables.Currencies.SHOW_ON_MAP},
                null,
                null,
                null,
                null,
                null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getContext(),
                R.layout.list_item_currency_filter,
                currencies,
                new String[] { Tables.Currencies.NAME, Tables.Currencies.SHOW_ON_MAP },
                new int[] { R.id.name, R.id.enabled },
                0);

        int idIndex = currencies.getColumnIndex(Tables.Currencies._ID);
        int showOnMapColumnIndex = currencies.getColumnIndex(Tables.Currencies.SHOW_ON_MAP);

        adapter.setViewBinder((view, cursor, columnIndex) -> {
            if (columnIndex == showOnMapColumnIndex) {
                CheckBox checkBox = (CheckBox) view;
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(cursor.getInt(showOnMapColumnIndex) > 0);
                String currencyId = cursor.getString(idIndex);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> setShowOnMap(currencyId, isChecked));

                return true;
            }

            return false;
        });

        return adapter;
    }

    private void setShowOnMap(String currencyId, boolean showOnMap) {
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Tables.Currencies.SHOW_ON_MAP, showOnMap ? 1 : 0);
        db.update(Tables.Currencies.TABLE_NAME, values, Tables.Currencies._ID + " = ?", new String[] { currencyId });
    }
}
