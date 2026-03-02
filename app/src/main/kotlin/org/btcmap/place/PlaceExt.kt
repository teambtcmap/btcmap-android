package org.btcmap.place

import org.btcmap.db.table.place.Place

fun Place.isMerchant(): Boolean {
    return icon != "local_atm" && icon != "currency_exchange"
}