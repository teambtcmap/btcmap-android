package com.bubelov.coins.ui.widget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.ListPopupWindow;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListAdapter;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;
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
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

        Cursor currencies = db.query(Tables.Currencies.TABLE_NAME,
                new String[]{ Tables.Currencies._ID, Tables.Currencies.NAME, Tables.Currencies.SHOW_ON_MAP },
                null,
                null,
                null,
                null,
                null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(context,
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

                ViewGroup parent = (ViewGroup) view.getParent();
                parent.setOnClickListener(v -> checkBox.toggle());

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
