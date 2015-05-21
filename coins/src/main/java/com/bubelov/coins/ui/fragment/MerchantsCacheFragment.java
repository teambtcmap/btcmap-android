package com.bubelov.coins.ui.fragment;

import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.MerchantsCache;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 18:37
 */

public class MerchantsCacheFragment extends AbstractFragment {
    public static final String TAG = MerchantsCacheFragment.class.getSimpleName();

    private MerchantsCache merchantsCache;

    public MerchantsCacheFragment() {
        super();
        SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();
        merchantsCache = new MerchantsCache(db);
        setRetainInstance(true);
    }

    public MerchantsCache getMerchantsCache() {
        return merchantsCache;
    }
}
