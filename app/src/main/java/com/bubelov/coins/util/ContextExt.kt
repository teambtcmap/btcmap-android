package com.bubelov.coins.util

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent

/**
 * @author Igor Bubelov
 */

fun Context.openUrl(url: String) {
    val urlBuilder = StringBuilder()

    if (url.startsWith("www.") || !url.contains("http")) {
        urlBuilder.append("http://")
    }

    urlBuilder.append(url)
    val intentBuilder = CustomTabsIntent.Builder()
    intentBuilder.setStartAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    intentBuilder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    val customTabsIntent = intentBuilder.build()
    customTabsIntent.launchUrl(this, Uri.parse(url))
}