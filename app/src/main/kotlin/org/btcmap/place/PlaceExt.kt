package org.btcmap.place

import org.btcmap.db.table.Place

fun Place.isMerchant(): Boolean {
    return icon != "local_atm" && icon != "currency_exchange"
}