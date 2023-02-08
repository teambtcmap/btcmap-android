package element

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias OsmTags = JsonObject

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