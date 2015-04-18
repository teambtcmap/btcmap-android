package com.bubelov.coins.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 11:54
 */

public class Utils {
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null
                && networkInfo.isAvailable()
                && networkInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }
}
