package org.btcmap.place

import org.junit.Assert
import org.junit.Test

class ReportPlaceTest {

    @Test
    fun fromValue_mapsEveryApiValue() {
        Assert.assertEquals(ReportType.Verified, ReportType.fromValue("verified"))
        Assert.assertEquals(ReportType.RefusedSats, ReportType.fromValue("refused_sats"))
        Assert.assertEquals(ReportType.OutOfBusiness, ReportType.fromValue("out_of_business"))
    }

    @Test
    fun fromValue_unknownOrNullIsNull() {
        Assert.assertNull(ReportType.fromValue(null))
        Assert.assertNull(ReportType.fromValue(""))
        Assert.assertNull(ReportType.fromValue("other"))
    }

    @Test
    fun comment_isTrimmedAndBlankBecomesNull() {
        Assert.assertEquals("hello", ReportDraft(ReportType.Verified, note = "  hello  ").comment)
        Assert.assertNull(ReportDraft(ReportType.Verified, note = "   ").comment)
        Assert.assertNull(ReportDraft(ReportType.Verified).comment)
    }
}
