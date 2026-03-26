package org.btcmap.i18n

import android.content.Context
import org.btcmap.R
import org.btcmap.db.table.Place
import java.util.Locale

fun Place.getLocalizedName(): String? {
    val locale = Locale.getDefault().language
    return localizedName?.get(locale)?.asString ?: name
}

fun Place.getLocalizedOpeningHours(context: Context): String? {
    val locale = Locale.getDefault().language

    val result = if (localizedOpeningHours != null) {
        val localized = localizedOpeningHours.get(locale)?.asString

        localized?.ifBlank {
            val en = localizedOpeningHours.get("en")?.asString

            en?.ifBlank {
                openingHours
            }
        }
    } else {
        openingHours
    }

    return result?.translate(context)
}

private fun String.translate(context: Context): String {
    return this
        .replace("Monday", context.getString(R.string.monday), ignoreCase = true)
        .replace("Tuesday", context.getString(R.string.tuesday), ignoreCase = true)
        .replace("Wednesday", context.getString(R.string.wednesday), ignoreCase = true)
        .replace("Thursday", context.getString(R.string.thursday), ignoreCase = true)
        .replace("Friday", context.getString(R.string.friday), ignoreCase = true)
        .replace("Saturday", context.getString(R.string.saturday), ignoreCase = true)
        .replace("Sunday", context.getString(R.string.sunday), ignoreCase = true)
        .replace("Closed", context.getString(R.string.closed).lowercase(), ignoreCase = true)
}