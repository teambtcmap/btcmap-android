package common

import android.annotation.SuppressLint
import java.util.*

enum class DistanceUnits {
    KILOMETERS,
    MILES;

    companion object {
        @SuppressLint("ConstantLocale")
        val default = Locale.getDefault().toDistanceUnits()

        private fun Locale.toDistanceUnits() = when(country) {
            in listOf("LR", "MM", "GB", "US") -> MILES
            else -> KILOMETERS
        }
    }
}