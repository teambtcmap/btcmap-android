package com.bubelov.coins.ui.widget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.ListPopupWindow;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.util.Utils;

/**
 * Author: Igor Bubelov
 * Date: 07/05/15 23:29
 */

public class CurrenciesFilterPopup extends ListPopupWindow {
    private Context context;

    public CurrenciesFilterPopup(Context context) {
        super(context);
        this.context = context;
        setAdapter(getAdapter());
        setModal(true);
        setWidth(Utils.dpToPx(context, 250));
    }

    public ListAdapter getAdapter() {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME,
                new String[]{DbContract.Currencies._ID, DbContract.Currencies.NAME, DbContract.Currencies.SHOW_ON_MAP},
                String.format("%s = ?", DbContract.Currencies.CRYPTO), new String[]{String.valueOf(1)},
                null,
                null,
                null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(context,
                R.layout.list_item_currency_filter,
                cursor,
                new String[]{DbContract.Currencies.NAME, DbContract.Currencies.SHOW_ON_MAP},
                new int[]{R.id.name, R.id.enabled},
                0);

        final int idIndex = 0;
        final int showOnMapColumnIndex = 2;

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == showOnMapColumnIndex) {
                    final CheckBox checkBox = (CheckBox) view;
                    checkBox.setOnCheckedChangeListener(null);
                    checkBox.setChecked(cursor.getInt(showOnMapColumnIndex) > 0);
                    final long currencyId = cursor.getLong(idIndex);
                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            setShowOnMap(currencyId, isChecked);
                        }
                    });

                    ViewGroup parent = (ViewGroup) view.getParent();
                    parent.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkBox.toggle();
                        }
                    });

                    return true;
                }

                return false;
            }
        });

        return adapter;
    }

    private void setShowOnMap(long currencyId, boolean showOnMap) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Currencies.SHOW_ON_MAP, showOnMap ? 1 : 0);

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        db.update(DbContract.Currencies.TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(currencyId)});
    }
}
