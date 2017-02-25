package com.bubelov.coins.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AlertDialog;

import java.util.List;

/**
 * @author Igor Bubelov
 */

public class Utils {
    public static void openUrl(Context context, String url) {
        if (url.startsWith("www.") || !url.contains("http")) {
            url = "http://" + url;
        }

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        builder.setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out);
        builder.setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out);

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    public static void share(Context context, String subject, String text) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    public static void showErrors(Context context, List<String> errors) {
        StringBuilder errorMessageBuilder = new StringBuilder();

        for (String error : errors) {
            errorMessageBuilder.append(error).append("\n");
        }

        new AlertDialog.Builder(context)
                .setMessage(errorMessageBuilder.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
