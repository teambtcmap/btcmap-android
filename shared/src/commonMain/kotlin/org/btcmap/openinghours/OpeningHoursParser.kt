package org.btcmap.openinghours

import kotlinx.datetime.DayOfWeek

/**
 * Parses an OpenStreetMap `opening_hours` value into a weekly schedule.
 *
 * Only the common, unambiguous part of the syntax is understood: day selectors
 * with ranges and lists, time ranges with lists, `24/7`, `off`, `closed` and
 * inline comments. Anything else — seasonal or month-based rules, nth-weekday
 * selectors, "by appointment" notes — returns null so the caller can fall back
 * to the raw value instead of showing a wrong week.
 */
fun String.toOpeningHours(): OpeningHours? {
    val cleaned = normalizePunctuation().stripComments().substringBefore("||").trim()
    if (cleaned.isEmpty()) return null

    val schedule: MutableMap<DayOfWeek, DaySchedule> =
        WEEKDAYS.associateWithTo(LinkedHashMap()) { DaySchedule.Closed }
    var applied = false

    for (segment in cleaned.split(';', '\n')) {
        val rule = segment.trim()
        if (rule.isEmpty()) continue

        // An unreadable rule — a season, a month, an nth weekday — makes the
        // whole value unsafe to summarize, so fall back to the raw text.
        val rules = parseSegment(rule) ?: return null

        for (parsed in rules) {
            // Overlapping normal rules and partial closures are combined the
            // way the OpenStreetMap specification prescribes. A combination the
            // weekly model cannot represent bails out to the raw value.
            if (!apply(schedule, parsed)) return null
            applied = true
        }
    }

    if (!applied) return null
    return OpeningHours(schedule)
}

/**
 * Applies one rule to the running week.
 *
 * A normal rule replaces the days it covers; an additional rule (one introduced
 * by a comma) adds to them. An `off`/`closed` rule instead cuts the times it
 * matches out of the days, or closes them entirely when it has no time
 * selector. Returns false when the result cannot be represented.
 */
private fun apply(schedule: MutableMap<DayOfWeek, DaySchedule>, rule: Rule): Boolean {
    for (day in rule.days) {
        val current = schedule.getValue(day)
        schedule[day] = when {
            rule.schedule is DaySchedule.Closed -> cut(current, rule.offTimes) ?: return false
            rule.additional -> union(current, rule.schedule)
            else -> rule.schedule
        }
    }
    return true
}

/** Adds [add] to [current], keeping the ranges of both. */
private fun union(current: DaySchedule, add: DaySchedule): DaySchedule = when {
    current is DaySchedule.Closed -> add
    current is DaySchedule.OpenAllDay || add is DaySchedule.OpenAllDay -> DaySchedule.OpenAllDay
    current is DaySchedule.Open && add is DaySchedule.Open -> {
        val ranges = LinkedHashSet<TimeRange>()
        ranges += current.ranges
        ranges += add.ranges
        DaySchedule.Open(ranges.toList())
    }

    else -> current
}

/** Removes [off] from [current]; null when the times cannot be represented. */
private fun cut(current: DaySchedule, off: List<TimeRange>?): DaySchedule? {
    if (off == null) return DaySchedule.Closed

    val open = current.toSpans() ?: return null
    if (open.isEmpty()) return DaySchedule.Closed

    return open.subtract(off.toSpans() ?: return null).toSchedule()
}

private data class Span(val start: Int, val end: Int)

private fun DaySchedule.toSpans(): List<Span>? = when (this) {
    DaySchedule.Closed -> emptyList()
    DaySchedule.OpenAllDay -> listOf(Span(0, DAY_MINUTES))
    is DaySchedule.Open -> ranges.toSpans()
}

private fun List<TimeRange>.toSpans(): List<Span>? {
    val spans = ArrayList<Span>(size)
    for (range in this) {
        val start = range.start.toMinutes() ?: return null
        val end = range.end?.toMinutes() ?: return null
        // A range that ends before it starts wraps past midnight.
        val fixedEnd = if (end <= start) end + DAY_MINUTES else end
        if (fixedEnd <= start) return null
        spans += Span(start, fixedEnd)
    }
    return spans
}

private fun List<Span>.subtract(cuts: List<Span>): List<Span> {
    var result = this
    for (cut in cuts) {
        val next = ArrayList<Span>()
        for (span in result) {
            if (cut.end <= span.start || cut.start >= span.end) {
                next += span
                continue
            }
            if (cut.start > span.start) next += Span(span.start, cut.start)
            if (cut.end < span.end) next += Span(cut.end, span.end)
        }
        result = next
    }
    return result
}

private fun List<Span>.toSchedule(): DaySchedule = when {
    isEmpty() -> DaySchedule.Closed
    size == 1 && this[0].start == 0 && this[0].end == DAY_MINUTES -> DaySchedule.OpenAllDay
    else -> DaySchedule.Open(map { it.toTimeRange() })
}

private fun Span.toTimeRange(): TimeRange {
    val start = formatMinutes(start)
    val end = if (end > DAY_MINUTES) formatMinutes(end - DAY_MINUTES) else formatMinutes(end)
    return TimeRange(start, end)
}

private fun String.toMinutes(): Int? {
    val parts = split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (minutes !in 0..59) return null
    return hours * 60 + minutes
}

private fun formatMinutes(minutes: Int): String =
    "${pad2(minutes / 60)}:${pad2(minutes % 60)}"

private fun pad2(value: Int): String = if (value < 10) "0$value" else value.toString()

private const val DAY_MINUTES = 24 * 60

private val WEEKDAYS: List<DayOfWeek> = DayOfWeek.entries

private val ALL_WEEKDAYS: Set<DayOfWeek> = LinkedHashSet(WEEKDAYS)

private val dayAbbreviations = mapOf(
    "mo" to DayOfWeek.MONDAY,
    "tu" to DayOfWeek.TUESDAY,
    "we" to DayOfWeek.WEDNESDAY,
    "th" to DayOfWeek.THURSDAY,
    "fr" to DayOfWeek.FRIDAY,
    "sa" to DayOfWeek.SATURDAY,
    "su" to DayOfWeek.SUNDAY,
)

private val timeRegex = Regex("""\d{1,2}:\d{2}""")

private val wordRegex = Regex("""[^\s,;:+\-"]+""")

/** Matches `24/7`, optionally followed by an `open`, `off` or `closed` modifier. */
private val aroundTheClockRegex =
    Regex("""^24/7(?:\s+(open|off|closed))?$""", RegexOption.IGNORE_CASE)

private sealed interface Token {
    data class Day(val day: DayOfWeek) : Token
    data class DayRange(val from: DayOfWeek, val to: DayOfWeek) : Token
    data object Holiday : Token
    data class Time(val range: TimeRange) : Token
    data object Comma : Token
    data class Word(val value: String) : Token
}

private data class Rule(
    val days: Set<DayOfWeek>,
    val schedule: DaySchedule,
    val additional: Boolean,
    /** The times an `off`/`closed` rule cuts out, or null to close the whole day. */
    val offTimes: List<TimeRange>? = null,
)

private class TokenCursor(private val tokens: List<Token>) {

    private var index = 0

    fun peek(offset: Int = 0): Token? = tokens.getOrNull(index + offset)

    fun hasMore(): Boolean = index < tokens.size

    fun take(): Token = tokens[index++]

    /** The day selector, an empty set for holidays only, or null for none. */
    fun takeDaySelector(): Set<DayOfWeek>? {
        val days = LinkedHashSet<DayOfWeek>()
        var found = false

        while (true) {
            when (val token = peek()) {
                is Token.Day -> {
                    take()
                    days += token.day
                    found = true
                }

                is Token.DayRange -> {
                    take()
                    days += token.days()
                    found = true
                }

                Token.Holiday -> {
                    take()
                    found = true
                }

                else -> break
            }

            val next = peek()
            val afterNext = peek(1)
            if (next == Token.Comma &&
                (afterNext is Token.Day || afterNext is Token.DayRange || afterNext == Token.Holiday)
            ) {
                take()
            } else {
                break
            }
        }

        return if (found) days else null
    }

    fun takeTimeSelector(): List<TimeRange>? {
        val ranges = ArrayList<TimeRange>()

        while (peek() is Token.Time) {
            ranges += (take() as Token.Time).range
            val next = peek()
            if (next == Token.Comma && peek(1) is Token.Time) take() else break
        }

        return ranges.ifEmpty { null }
    }

    fun takeModifier(): String? {
        val word = peek() as? Token.Word ?: return null
        val value = word.value.lowercase()
        if (value != "off" && value != "closed" && value != "open") return null
        take()
        return value
    }
}

private fun Token.DayRange.days(): Set<DayOfWeek> {
    val days = LinkedHashSet<DayOfWeek>()
    var index = from.ordinal
    while (true) {
        val day = DayOfWeek.entries[index]
        days += day
        if (day == to) break
        index = (index + 1) % DayOfWeek.entries.size
    }
    return days
}

private fun parseSegment(segment: String): List<Rule>? {
    parseAroundTheClock(segment)?.let { return it }

    val tokens = tokenize(segment) ?: return null
    if (tokens.isEmpty()) return null

    val cursor = TokenCursor(tokens)
    val rules = ArrayList<Rule>()
    var additional = false

    while (true) {
        val days = cursor.takeDaySelector()
        val times = cursor.takeTimeSelector()
        val modifier = cursor.takeModifier()

        val schedule = when {
            modifier == "off" || modifier == "closed" -> DaySchedule.Closed
            times != null -> scheduleOf(times)
            days != null && modifier == "open" -> DaySchedule.OpenAllDay
            else -> return null
        }

        // A holiday-only selector carries no weekday information.
        if (days == null || days.isNotEmpty()) {
            rules += Rule(
                days = days ?: ALL_WEEKDAYS,
                schedule = schedule,
                additional = additional,
                offTimes = if (schedule is DaySchedule.Closed) times else null,
            )
        }

        if (cursor.peek() == Token.Comma) {
            cursor.take()
            additional = true
        } else if (cursor.hasMore()) {
            return null
        } else {
            break
        }
    }

    return rules
}

private fun parseAroundTheClock(segment: String): List<Rule>? {
    val match = aroundTheClockRegex.matchEntire(segment) ?: return null
    val modifier = match.groupValues[1].lowercase()
    // "24/7 off" is contradictory; let the caller fall back to the raw value.
    if (modifier == "off" || modifier == "closed") return null
    return listOf(Rule(ALL_WEEKDAYS, DaySchedule.OpenAllDay, additional = false))
}

private fun scheduleOf(ranges: List<TimeRange>): DaySchedule =
    if (ranges.size == 1 && ranges.single().isAllDay) {
        DaySchedule.OpenAllDay
    } else {
        DaySchedule.Open(ranges)
    }

private fun tokenize(input: String): List<Token>? {
    val tokens = ArrayList<Token>()
    var index = 0

    while (index < input.length) {
        val char = input[index]
        when {
            char.isWhitespace() -> index++

            char == ',' -> {
                tokens += Token.Comma
                index++
            }

            char.isDigit() -> {
                val time = matchTime(input, index) ?: return null
                tokens += Token.Time(time.value)
                index = time.endIndex
            }

            else -> {
                val day = matchDay(input, index)
                if (day != null) {
                    val range = matchDayRange(input, index, day)
                    if (range != null) {
                        tokens += Token.DayRange(day, range.first)
                        index = range.second
                    } else {
                        tokens += Token.Day(day)
                        index = skipDot(input, index + 2)
                    }
                } else if (input.regionMatches(index, "PH", 0, 2, ignoreCase = true) ||
                    input.regionMatches(index, "SH", 0, 2, ignoreCase = true)
                ) {
                    tokens += Token.Holiday
                    index += 2
                } else {
                    val word = wordRegex.find(input, index)
                    if (word == null || word.range.first != index) return null
                    tokens += Token.Word(word.value)
                    index = word.range.last + 1
                }
            }
        }
    }

    return tokens
}

/** Matches "Mo-Fr" (dots as in "Mo.-Fr." are tolerated) starting at [index]. */
private fun matchDayRange(input: String, index: Int, from: DayOfWeek): Pair<DayOfWeek, Int>? {
    var cursor = skipDotsAndSpaces(input, index + 2)
    if (input.getOrNull(cursor) != '-') return null

    cursor = skipDotsAndSpaces(input, cursor + 1)
    val to = matchDay(input, cursor) ?: return null
    return to to skipDot(input, cursor + 2)
}

private fun matchDay(input: String, index: Int): DayOfWeek? {
    if (index + 2 > input.length) return null
    return dayAbbreviations[input.substring(index, index + 2).lowercase()]
}

private fun skipDot(input: String, index: Int): Int =
    if (input.getOrNull(index) == '.') index + 1 else index

private fun skipDotsAndSpaces(input: String, start: Int): Int {
    var index = start
    while (index < input.length && (input[index] == '.' || input[index].isWhitespace())) index++
    return index
}

private data class Matched<T>(val value: T, val endIndex: Int)

private fun matchTime(input: String, start: Int): Matched<TimeRange>? {
    val first = timeRegex.find(input, start) ?: return null
    if (first.range.first != start) return null
    val startTime = normalizeTime(first.value) ?: return null

    var cursor = first.range.last + 1
    while (cursor < input.length && input[cursor].isWhitespace()) cursor++

    when (input.getOrNull(cursor)) {
        '+' -> return Matched(TimeRange(startTime, null), cursor + 1)

        '-' -> {
            cursor++
            while (cursor < input.length && input[cursor].isWhitespace()) cursor++
            val second = timeRegex.find(input, cursor) ?: return null
            if (second.range.first != cursor) return null
            val endTime = normalizeTime(second.value) ?: return null

            var end = second.range.last + 1
            var plus = end
            while (plus < input.length && input[plus].isWhitespace()) plus++
            // "18:00+" marks an uncertain end; treat it as the given end.
            if (input.getOrNull(plus) == '+') end = plus + 1
            return Matched(TimeRange(startTime, endTime), end)
        }
    }

    // A lone time is an open-ended span.
    return Matched(TimeRange(startTime, null), cursor)
}

private fun normalizeTime(value: String): String? {
    val parts = value.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hours !in 0..24 || minutes !in 0..59) return null
    if (hours == 24 && minutes != 0) return null
    return "${pad2(hours)}:${pad2(minutes)}"
}

private fun String.normalizePunctuation(): String = buildString(length) {
    for (char in this@normalizePunctuation) {
        append(
            when (char) {
                '\u00a0', '\u2007', '\u2009', '\u202f' -> ' '
                '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2212' -> '-'
                else -> char
            }
        )
    }
}

private fun String.stripComments(): String = buildString(length) {
    var inComment = false
    for (char in this@stripComments) {
        when {
            char == '"' -> inComment = !inComment
            !inComment -> append(char)
        }
    }
}
