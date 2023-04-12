package element

import android.content.res.Resources
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.R
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

typealias OsmTags = JsonObject

fun OsmTags.name(
    res: Resources,
    locale: Locale = Locale.getDefault(),
): String {
    return name(
        atmLocalizedString = res.getString(R.string.atm),
        unnamedPlaceLocalizedString = res.getString(R.string.unnamed_place),
        locale = locale,
    )
}

fun OsmTags.name(
    atmLocalizedString: String,
    unnamedPlaceLocalizedString: String,
    locale: Locale = Locale.getDefault(),
): String {
    val countryCode = locale.language
    val localizedName = this["name:$countryCode"]?.jsonPrimitive?.contentOrNull ?: ""
    val name = this["name"]?.jsonPrimitive?.contentOrNull ?: ""
    val amenity = this["amenity"]?.jsonPrimitive?.contentOrNull ?: ""

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

    this["survey:date"]?.jsonPrimitive?.contentOrNull?.let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    this["check_date"]?.jsonPrimitive?.contentOrNull?.let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    this["check_date:currency:XBT"]?.jsonPrimitive?.contentOrNull?.let { rawDate ->
        runCatching { LocalDate.parse(rawDate) }
            .onSuccess { validVerificationDates += it }
    }

    return validVerificationDates.maxOrNull()?.atStartOfDay(ZoneOffset.UTC)
}