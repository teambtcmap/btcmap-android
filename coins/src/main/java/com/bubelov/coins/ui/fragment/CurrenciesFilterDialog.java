package com.bubelov.coins.ui.fragment;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        ListView listView = (ListView) getLayoutInflater().inflate(R.layout.dialog_currencies_filter, null);

        ListAdapter adapter = new SimpleAdapter(getContext(),
                getData(),
                R.layout.list_item_currency_filter,
                new String[] { "name", "enabled" },
                new int[] { R.id.name, R.id.enabled });

        listView.setAdapter(adapter);
        setContentView(listView);
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> data = new ArrayList<>();
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

        Cursor cursor = db.query(Tables.Currencies.TABLE_NAME,
                new String[]{Tables.Currencies._ID, Tables.Currencies.NAME, Tables.Currencies.CODE},
                null,
                null,
                null,
                null,
                null);

        Log.d(TAG, cursor.getCount() + " currencies found in DB");

        while (cursor.moveToNext()) {
            Map<String, Object> currency = new HashMap<>();
            currency.put("name", cursor.getString(cursor.getColumnIndex(Tables.Currencies.NAME)));
            currency.put("enabled", true);
            data.add(currency);
        }

        cursor.close();
        return data;
    }
}
