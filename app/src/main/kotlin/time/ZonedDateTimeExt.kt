package time

import java.time.ZoneOffset
import java.time.ZonedDateTime

fun now(zone: ZoneOffset = ZoneOffset.UTC) = ZonedDateTime.now(zone)

fun String.toZonedDateTime() = ZonedDateTime.parse(this)