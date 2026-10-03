package org.btcmap.ui

import androidx.compose.ui.text.style.TextDecoration
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The place sheet's opening-hours text: the parsed week, today underlined. */
class OpeningHoursTextTest {

    @Test
    fun parsedWeek_isShownOneLinePerDay() {
        val text = openingHoursText(
            raw = "Mo-Fr 09:00-17:00",
            closedLabel = "Closed",
            aroundTheClockLabel = "Open 24/7",
            today = DayOfWeek.WEDNESDAY,
            language = "en",
        ).text

        assertEquals(
            listOf(
                "Monday: 09:00–17:00",
                "Tuesday: 09:00–17:00",
                "Wednesday: 09:00–17:00",
                "Thursday: 09:00–17:00",
                "Friday: 09:00–17:00",
                "Saturday: Closed",
                "Sunday: Closed",
            ),
            text.lines(),
        )
    }

    @Test
    fun todaysLine_isUnderlined() {
        val text = openingHoursText(
            raw = "Mo-Fr 09:00-17:00",
            closedLabel = "Closed",
            aroundTheClockLabel = "Open 24/7",
            today = DayOfWeek.WEDNESDAY,
            language = "en",
        )

        val underlined = text.spanStyles
            .filter { it.item.textDecoration == TextDecoration.Underline }
            .map { text.text.substring(it.start, it.end) }

        assertEquals(listOf("Wednesday: 09:00–17:00"), underlined)
    }

    @Test
    fun aroundTheClock_isShownAsASingleLabel() {
        val text = openingHoursText(
            raw = "24/7",
            closedLabel = "Closed",
            aroundTheClockLabel = "Open 24/7",
            today = DayOfWeek.MONDAY,
            language = "en",
        )

        assertEquals("Open 24/7", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }

    @Test
    fun unparseableValue_fallsBackToTheRawText() {
        val text = openingHoursText(
            raw = "Apr-Oct",
            closedLabel = "Closed",
            aroundTheClockLabel = "Open 24/7",
            today = DayOfWeek.MONDAY,
            language = "en",
        )

        assertEquals("Apr-Oct", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }
}
