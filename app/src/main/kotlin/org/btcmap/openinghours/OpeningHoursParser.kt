package org.btcmap.openinghours

import java.time.DayOfWeek

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

    if (cleaned.equals("24/7", ignoreCase = true)) {
        return OpeningHours(WEEKDAYS.associateWith { DaySchedule.OpenAllDay })
    }

    val schedule: MutableMap<DayOfWeek, DaySchedule?> =
        WEEKDAYS.associateWithTo(LinkedHashMap()) { null as DaySchedule? }
    var applied = false

    for (segment in cleaned.split(';', '\n')) {
        val rule = segment.trim()
        if (rule.isEmpty()) continue

        // An unreadable rule — a season, a month, an nth weekday — makes the
        // whole value unsafe to summarize, so fall back to the raw text.
        val rules = parseSegment(rule) ?: return null

        for (parsed in rules) {
            for (day in parsed.days) schedule[day] = parsed.schedule
            applied = true
        }
    }

    if (!applied) return null
    return OpeningHours(schedule.mapValues { it.value ?: DaySchedule.Closed })
}

private val WEEKDAYS: List<DayOfWeek> = DayOfWeek.values().toList()

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

private val wordRegex = Regex("""[^\s,;:+\-–—"]+""")

private sealed interface Token {
    data class Day(val day: DayOfWeek) : Token
    data class DayRange(val from: DayOfWeek, val to: DayOfWeek) : Token
    data object Holiday : Token
    data class Time(val range: TimeRange) : Token
    data object Comma : Token
    data class Word(val value: String) : Token
}

private data class Rule(val days: Set<DayOfWeek>, val schedule: DaySchedule)

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
    var day = from
    while (true) {
        days += day
        if (day == to) break
        day = day.plus(1)
    }
    return days
}

private fun parseSegment(segment: String): List<Rule>? {
    if (segment.equals("24/7", ignoreCase = true)) {
        return listOf(Rule(ALL_WEEKDAYS, DaySchedule.OpenAllDay))
    }

    val tokens = tokenize(segment) ?: return null
    if (tokens.isEmpty()) return null

    val cursor = TokenCursor(tokens)
    val rules = ArrayList<Rule>()

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

        when {
            days == null -> rules += Rule(ALL_WEEKDAYS, schedule)
            days.isNotEmpty() -> rules += Rule(days, schedule)
            // A holiday-only selector carries no weekday information.
        }

        if (cursor.peek() == Token.Comma) {
            cursor.take()
        } else if (cursor.hasMore()) {
            return null
        } else {
            break
        }
    }

    return rules
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
                        index += 2
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

/** Matches "Mo-Fr" starting at [index], returning the end day and next index. */
private fun matchDayRange(input: String, index: Int, from: DayOfWeek): Pair<DayOfWeek, Int>? {
    var cursor = index + 2
    while (cursor < input.length && input[cursor].isWhitespace()) cursor++
    if (input.getOrNull(cursor) != '-') return null

    cursor++
    while (cursor < input.length && input[cursor].isWhitespace()) cursor++
    val to = matchDay(input, cursor) ?: return null
    return to to (cursor + 2)
}

private fun matchDay(input: String, index: Int): DayOfWeek? {
    if (index + 2 > input.length) return null
    return dayAbbreviations[input.substring(index, index + 2).lowercase()]
}

private class Matched<T>(val value: T, val endIndex: Int)

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
    return "%02d:%02d".format(hours, minutes)
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
