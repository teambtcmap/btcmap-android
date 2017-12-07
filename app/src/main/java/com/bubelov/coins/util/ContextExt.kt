package com.bubelov.coins.util

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

fun Context.openUrl(url: String): Boolean {
    val urlBuilder = StringBuilder()

    if (url.startsWith("www.") || !url.contains("http")) {
        urlBuilder.append("http://")
    }

    urlBuilder.append(url)
    val intentBuilder = CustomTabsIntent.Builder()
    intentBuilder.setStartAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    intentBuilder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    val customTabsIntent = intentBuilder.build()

    return try {
        customTabsIntent.launchUrl(this, Uri.parse(urlBuilder.toString()))
        true
    } catch (e : Exception) {
        Timber.e(e)
        false
    }
}