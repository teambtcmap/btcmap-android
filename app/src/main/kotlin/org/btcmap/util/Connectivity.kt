package org.btcmap.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

/**
 * Whether the device currently has a validated internet connection.
 *
 * Requires both [NetworkCapabilities.NET_CAPABILITY_INTERNET] and
 * [NetworkCapabilities.NET_CAPABILITY_VALIDATED], so a network that is up but
 * cannot actually reach the internet (a captive portal, say) reads as offline.
 * This is the single definition of "online" the map uses to switch the bundled
 * basemap between its split and all-overzoomed behaviour.
 */
fun Context.isOnline(): Boolean {
    val cm = getSystemService<ConnectivityManager>() ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
