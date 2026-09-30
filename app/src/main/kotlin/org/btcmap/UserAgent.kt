package org.btcmap

/**
 * Identifies this app and its version to the BTC Map API: sent as the HTTP
 * User-Agent and recorded as the `origin` of a place report or submission.
 */
val userAgent = "BTC Map Android ${BuildConfig.VERSION_CODE}"
