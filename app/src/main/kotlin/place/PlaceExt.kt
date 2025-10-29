package place

import db.table.place.Place

fun Place.isMerchant(): Boolean {
    return icon != "local_atm" && icon != "currency_exchange"
}