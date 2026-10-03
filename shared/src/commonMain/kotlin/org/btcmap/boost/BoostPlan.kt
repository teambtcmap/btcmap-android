package org.btcmap.boost

import org.btcmap.api.PlaceBoostQuoteResponse

/**
 * The boost durations the API accepts, in the order they are offered.
 *
 * The API key is the enum [name] and the quoted prices are looked up here, so
 * Android and the desktop cannot offer different durations or read the wrong
 * quote field. The labels stay with each host, because Android resolves them
 * from its string resources.
 */
enum class BoostPlan(val days: Long) {
    ONE_MONTH(30L),
    THREE_MONTHS(90L),
    TWELVE_MONTHS(365L),
    ;

    /** The quoted price for this duration. */
    fun priceSat(quote: PlaceBoostQuoteResponse): Long = when (this) {
        ONE_MONTH -> quote.quote30dSat
        THREE_MONTHS -> quote.quote90dSat
        TWELVE_MONTHS -> quote.quote365dSat
    }
}
