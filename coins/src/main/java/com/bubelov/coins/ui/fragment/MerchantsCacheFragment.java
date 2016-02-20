package com.bubelov.coins.ui.fragment;

import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;

import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.database.Database;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 18:37
 */

public class MerchantsCacheFragment extends Fragment {
    public static final String TAG = MerchantsCacheFragment.class.getSimpleName();

    private MerchantsCache merchantsCache;

    public MerchantsCacheFragment() {
        super();
        SQLiteDatabase db = Database.get();
        merchantsCache = new MerchantsCache(db);
        setRetainInstance(true);
    }

    public MerchantsCache getMerchantsCache() {
        return merchantsCache;
    }
}
