package element

import android.content.res.Resources
import org.btcmap.R
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

typealias OsmTags = JSONObject

fun OsmTags.name(
    res: Resources,
    locale: Locale = Locale.getDefault(),
): String {
    return name(
        atmLocalizedString = res.getString(R.string.atm),
        unnamedPlaceLocalizedString = res.getString(R.string.unnamed),
        locale = locale,
    )
}

fun OsmTags.name(
    atmLocalizedString: String,
    unnamedPlaceLocalizedString: String,
    locale: Locale = Locale.getDefault(),
): String {
    val countryCode = locale.language
    val localizedName = this.optString("name:$countryCode")
    val name = this.optString("name")
    val amenity = this.optString("amenity")

    return localizedName.ifBlank {
        name.ifBlank {
            if (amenity == "atm") {
                atmLocalizedString
            } else {
                unnamedPlaceLocalizedString
            }
        }
    }
}

fun OsmTags.bitcoinSurveyDate(): ZonedDateTime? {
    val validVerificationDates = mutableListOf<LocalDate>()

    this.optString("survey:date").let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    this.optString("check_date").let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    this.optString("check_date:currency:XBT").let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    return validVerificationDates.maxOrNull()?.atStartOfDay(ZoneOffset.UTC)
}