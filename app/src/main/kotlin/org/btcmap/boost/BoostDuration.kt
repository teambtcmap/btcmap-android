package org.btcmap.boost

import androidx.annotation.StringRes
import org.btcmap.R
import org.btcmap.api.PlaceBoostQuoteResponse

/**
 * The boost durations the API accepts, in the order they are offered, each with
 * its label and quoted price.
 */
internal enum class BoostDuration(
    val days: Long,
    @StringRes val labelRes: Int,
) {
    ONE_MONTH(30L, R.string.months_1),
    THREE_MONTHS(90L, R.string.months_3),
    TWELVE_MONTHS(365L, R.string.months_12),
    ;

    /** The quoted price for this duration. */
    fun priceSat(quote: PlaceBoostQuoteResponse): Long = when (this) {
        ONE_MONTH -> quote.quote30dSat
        THREE_MONTHS -> quote.quote90dSat
        TWELVE_MONTHS -> quote.quote365dSat
    }
}
