package com.bubelov.coins.ui.widget;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.ListPopupWindow;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListAdapter;

import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
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
        Cursor currencies = context.getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID, Database.Currencies.NAME, Database.Currencies.SHOW_ON_MAP},
                String.format("%s = ?", Database.Currencies.CRYPTO),
                new String[]{String.valueOf(1)},
                null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(context,
                R.layout.list_item_currency_filter,
                currencies,
                new String[] { Database.Currencies.NAME, Database.Currencies.SHOW_ON_MAP },
                new int[] { R.id.name, R.id.enabled },
                0);

        int idIndex = currencies.getColumnIndex(Database.Currencies._ID);
        int showOnMapColumnIndex = currencies.getColumnIndex(Database.Currencies.SHOW_ON_MAP);

        adapter.setViewBinder((view, cursor, columnIndex) -> {
            if (columnIndex == showOnMapColumnIndex) {
                CheckBox checkBox = (CheckBox) view;
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(cursor.getInt(showOnMapColumnIndex) > 0);
                long currencyId = cursor.getLong(idIndex);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> setShowOnMap(currencyId, isChecked));

                ViewGroup parent = (ViewGroup) view.getParent();
                parent.setOnClickListener(v -> checkBox.toggle());

                return true;
            }

            return false;
        });

        return adapter;
    }

    private void setShowOnMap(long currencyId, boolean showOnMap) {
        ContentValues values = new ContentValues();
        values.put(Database.Currencies.SHOW_ON_MAP, showOnMap ? 1 : 0);
        context.getContentResolver().update(ContentUris.withAppendedId(Database.Currencies.CONTENT_URI, currencyId), values, null, null);
    }
}
