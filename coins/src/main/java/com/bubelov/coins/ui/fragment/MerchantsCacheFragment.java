package com.bubelov.coins.ui.fragment;

import android.support.v4.app.Fragment;

import com.bubelov.coins.App;
import com.bubelov.coins.MerchantsCache;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 18:37
 */

public class MerchantsCacheFragment extends Fragment {
    public static final String TAG = MerchantsCacheFragment.class.getSimpleName();

    private MerchantsCache merchantsCache;

    public MerchantsCacheFragment() {
        super();
        merchantsCache = new MerchantsCache(App.getInstance().getDatabaseHelper().getReadableDatabase());
        setRetainInstance(true);
    }

    public MerchantsCache getMerchantsCache() {
        return merchantsCache;
    }
}
