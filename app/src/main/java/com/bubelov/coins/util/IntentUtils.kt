package com.bubelov.coins.util

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent

/**
 * @author Igor Bubelov
 */

object IntentUtils {
    fun openUrl(context: Context, url: String) {
        val urlBuilder = StringBuilder()

        if (url.startsWith("www.") || !url.contains("http")) {
            urlBuilder.append("http://")
        }

        urlBuilder.append(url)
        val intentBuilder = CustomTabsIntent.Builder()
        intentBuilder.setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        intentBuilder.setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        val customTabsIntent = intentBuilder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}