package org.btcmap.openinghours

import org.junit.Assert
import org.junit.Test
import kotlinx.datetime.DayOfWeek

class OpeningHoursTest {

    @Test
    fun toOpeningHours_aroundTheClock_showsSingleLine() {
        val hours = "24/7".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertTrue(hours!!.isOpenAroundTheClock)
        Assert.assertEquals(
            "Open 24/7",
            hours.toDisplayString(language = "en"),
        )
    }

    @Test
    fun toOpeningHours_midnightToMidnight_isTreatedAsAroundTheClock() {
        val hours = "Mo-Su 00:00-24:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertTrue(hours!!.isOpenAroundTheClock)
    }

    @Test
    fun toOpeningHours_weekdayRange_leavesTheWeekendClosed() {
        val hours = "Mo-Fr 09:00-17:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "09:00–17:00",
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
        Assert.assertEquals(DaySchedule.Closed, hours.days[DayOfWeek.SATURDAY])
        Assert.assertEquals(DaySchedule.Closed, hours.days[DayOfWeek.SUNDAY])
    }

    @Test
    fun toDisplayString_weekdayRange_listsEveryDay() {
        val display = "Mo-Fr 09:00-17:00".toOpeningHours()!!
            .toDisplayString(language = "en")

        Assert.assertEquals(
            """
            Monday: 09:00–17:00
            Tuesday: 09:00–17:00
            Wednesday: 09:00–17:00
            Thursday: 09:00–17:00
            Friday: 09:00–17:00
            Saturday: Closed
            Sunday: Closed
            """.trimIndent(),
            display,
        )
    }

    @Test
    fun toDisplayString_semicolonRules_overrideDayRanges() {
        val display = "Mo-Sa 11:00-20:00; Su 12:00-18:00".toOpeningHours()!!
            .toDisplayString(language = "en")

        Assert.assertTrue(display.startsWith("Monday: 11:00–20:00"))
        Assert.assertTrue(display.endsWith("Sunday: 12:00–18:00"))
    }

    @Test
    fun toOpeningHours_multipleRangesPerDay_keepsThemAll() {
        val hours = "Mo-Fr 10:00-14:00,16:00-20:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            listOf("10:00–14:00", "16:00–20:00"),
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
    }

    @Test
    fun toOpeningHours_commaSeparatedRules_withoutSemicolon() {
        val hours = "Mo-Th 08:00-17:00, Fr 08:00-13:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "08:00–17:00",
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
        Assert.assertEquals(
            "08:00–13:00",
            (hours.days[DayOfWeek.FRIDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_offRule_closesTheDays() {
        val hours = "Mo-Fr 09:00-17:00; Sa 09:00-15:00; Su off".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(DaySchedule.Closed, hours!!.days[DayOfWeek.SUNDAY])
        Assert.assertEquals(
            "09:00–15:00",
            (hours.days[DayOfWeek.SATURDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_wrappingRange_coversTheWeekBoundary() {
        val hours = "We-Mo 10:00-17:00; Tu off".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(DaySchedule.Closed, hours!!.days[DayOfWeek.TUESDAY])
        Assert.assertTrue(hours.days[DayOfWeek.WEDNESDAY] is DaySchedule.Open)
        Assert.assertTrue(hours.days[DayOfWeek.MONDAY] is DaySchedule.Open)
    }

    @Test
    fun toOpeningHours_holidaySelector_isIgnoredForTheWeek() {
        val hours = "Mo-Su,PH 07:00-22:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "07:00–22:00",
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_crossingMidnight_keepsTheRange() {
        val hours = "Tu-Su 17:00-01:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "17:00–01:00",
            (hours!!.days[DayOfWeek.TUESDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_normalisesSingleDigitHours() {
        val hours = "Mo-Fr 8:00-17:30".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "08:00–17:30",
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_monthRule_fallsBackToNull() {
        Assert.assertNull("Apr-Oct".toOpeningHours())
        Assert.assertNull("Mo-Su 10:00-19:00; Sep-Jun off".toOpeningHours())
    }

    @Test
    fun toOpeningHours_appointmentOnly_fallsBackToNull() {
        Assert.assertNull("\"By appointment\"".toOpeningHours())
    }

    @Test
    fun toOpeningHours_appointmentNote_afterRealHours_keepsTheHours() {
        val hours = "Mo-Fr 09:00-17:00 \"by appointment only\"".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertTrue(hours!!.days[DayOfWeek.MONDAY] is DaySchedule.Open)
    }

    @Test
    fun toOpeningHours_nthWeekday_fallsBackToNull() {
        Assert.assertNull("Su[1] 14:00-18:00 open".toOpeningHours())
    }

    @Test
    fun toOpeningHours_closedOnly() {
        val hours = "closed".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertTrue(hours!!.isAlwaysClosed)
        Assert.assertEquals(
            "Closed",
            hours.toDisplayString(language = "en"),
        )
    }

    @Test
    fun toDisplayString_usesTheGivenLocale() {
        val hours = "Mo-Fr 09:00-17:00".toOpeningHours()!!

        Assert.assertTrue(
            hours.toDisplayString(language = "de").startsWith("Montag: 09:00–17:00"),
        )
        Assert.assertTrue(
            "Понедельник: 09:00–17:00" in
                hours.toDisplayString(language = "ru"),
        )
    }

    @Test
    fun toOpeningHours_malformedRule_fallsBackToNull() {
        Assert.assertNull("Mo-Fr 09:00-17:00; banana".toOpeningHours())
        Assert.assertNull("Mo-Fr 09:00-17:00 Sa 09:00-14:00".toOpeningHours())
    }

    @Test
    fun toOpeningHours_empty_fallsBackToNull() {
        Assert.assertNull("".toOpeningHours())
        Assert.assertNull("   ".toOpeningHours())
    }

    @Test
    fun todayLineIndex_mapsEachDayToItsLine() {
        val hours = "Mo-Fr 09:00-17:00".toOpeningHours()!!

        Assert.assertEquals(0, hours.todayLineIndex(DayOfWeek.MONDAY))
        Assert.assertEquals(4, hours.todayLineIndex(DayOfWeek.FRIDAY))
        Assert.assertEquals(6, hours.todayLineIndex(DayOfWeek.SUNDAY))
    }

    @Test
    fun todayLineIndex_singleLineWeek_hasNoLine() {
        Assert.assertNull("24/7".toOpeningHours()!!.todayLineIndex(DayOfWeek.MONDAY))
        Assert.assertNull("closed".toOpeningHours()!!.todayLineIndex(DayOfWeek.MONDAY))
        Assert.assertNull(
            "Mo-Su 00:00-24:00".toOpeningHours()!!.todayLineIndex(DayOfWeek.WEDNESDAY),
        )
    }

    // Guards the range normalization against a helper that changes shape.
    @Test
    fun timeRange_isAllDay() {
        Assert.assertTrue(TimeRange("00:00", "24:00").isAllDay)
        Assert.assertFalse(TimeRange("00:00", null).isAllDay)
        Assert.assertFalse(TimeRange("00:00", "18:00").isAllDay)
    }

    @Test
    fun toOpeningHours_commaRuleAddsToEarlierHours() {
        // "Mo-Sa 09:00-12:00, We 15:00-18:00" is open Wednesday morning and
        // afternoon; a semicolon would instead make the later rule override.
        val hours = "Mo-Sa 09:00-12:00, We 15:00-18:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            listOf("09:00–12:00", "15:00–18:00"),
            (hours!!.days[DayOfWeek.WEDNESDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
        Assert.assertEquals(
            listOf("09:00–12:00"),
            (hours.days[DayOfWeek.SATURDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
    }

    @Test
    fun toOpeningHours_semicolonRuleOverridesEarlierHours() {
        val hours = "Mo-Sa 09:00-12:00; We 15:00-18:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            listOf("15:00–18:00"),
            (hours!!.days[DayOfWeek.WEDNESDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
    }

    @Test
    fun toOpeningHours_partialOff_cutsOnlyTheGivenTimes() {
        val hours = "Mo-Fr 09:00-17:00; We 12:00-13:00 off".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            listOf("09:00–12:00", "13:00–17:00"),
            (hours!!.days[DayOfWeek.WEDNESDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
        Assert.assertEquals(
            listOf("09:00–17:00"),
            (hours.days[DayOfWeek.THURSDAY] as DaySchedule.Open).ranges.map { it.format() },
        )
    }

    @Test
    fun toOpeningHours_trailingDots_areAccepted() {
        val hours = "Mo.-Fr. 09:00-17:00".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertEquals(
            "09:00–17:00",
            (hours!!.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
        Assert.assertEquals(DaySchedule.Closed, hours.days[DayOfWeek.SATURDAY])
    }

    @Test
    fun toOpeningHours_aroundTheClockWithModifier() {
        val hours = "24/7 open".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertTrue(hours!!.isOpenAroundTheClock)
    }

    @Test
    fun toOpeningHours_openEndedSpan_isNotTreatedAsAllDay() {
        val hours = "Mo-Su 00:00+".toOpeningHours()

        Assert.assertNotNull(hours)
        Assert.assertFalse(hours!!.isOpenAroundTheClock)
        Assert.assertEquals(
            "00:00+",
            (hours.days[DayOfWeek.MONDAY] as DaySchedule.Open).ranges.single().format(),
        )
    }

    @Test
    fun toOpeningHours_aroundTheClockOff_fallsBackToNull() {
        Assert.assertNull("24/7 off".toOpeningHours())
    }
}
