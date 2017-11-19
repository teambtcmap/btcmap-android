package com.bubelov.coins.util

/**
 * @author Igor Bubelov
 */

fun String.currencyCodeToName() : String = when(this) {
    "BTC" -> "Bitcoin"
    else -> this
}