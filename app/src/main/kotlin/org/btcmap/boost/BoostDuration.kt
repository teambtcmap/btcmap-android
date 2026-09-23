package org.btcmap.boost

import androidx.annotation.StringRes
import org.btcmap.R
import org.btcmap.api.PlaceBoostQuoteResponse

/**
 * The boost durations the API accepts, in the order they are offered. Each one
 * is bound to the radio button ([buttonId]) and label ([labelRes]) that
 * represent it on the boost screen, so the selection and its quote stay in one
 * place instead of being repeated across the fragment.
 */
internal enum class BoostDuration(
    val days: Long,
    @StringRes val labelRes: Int,
    val buttonId: Int,
) {
    ONE_MONTH(30L, R.string.months_1, R.id.boost_1m),
    THREE_MONTHS(90L, R.string.months_3, R.id.boost_3m),
    TWELVE_MONTHS(365L, R.string.months_12, R.id.boost_12m),
    ;

    /** The quoted price for this duration. */
    fun priceSat(quote: PlaceBoostQuoteResponse): Long = when (this) {
        ONE_MONTH -> quote.quote30dSat
        THREE_MONTHS -> quote.quote90dSat
        TWELVE_MONTHS -> quote.quote365dSat
    }

    companion object {
        /** The duration the radio button with [buttonId] stands for, if any. */
        fun fromButtonId(buttonId: Int): BoostDuration? =
            entries.firstOrNull { it.buttonId == buttonId }
    }
}
