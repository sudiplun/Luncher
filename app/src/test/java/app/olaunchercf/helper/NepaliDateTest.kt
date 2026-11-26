package app.olaunchercf.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NepaliDateTest {

    private val nepalTz: TimeZone = TimeZone.getTimeZone("Asia/Kathmandu")

    private fun adDate(year: Int, monthOneBased: Int, day: Int, tz: TimeZone = nepalTz): java.util.Date {
        return Calendar.getInstance(tz).apply {
            clear()
            set(year, monthOneBased - 1, day, 12, 0, 0)
        }.time
    }

    @Test
    fun anchorAndRecentDatesConvertAccurately() {
        assertEquals(
            NepaliDate.BsDate(2000, 1, 1, Calendar.WEDNESDAY),
            NepaliDate.toBsDate(adDate(1943, 4, 14), nepalTz)
        )
        assertEquals(
            NepaliDate.BsDate(2081, 1, 1, Calendar.SATURDAY),
            NepaliDate.toBsDate(adDate(2024, 4, 13), nepalTz)
        )
        assertEquals(
            NepaliDate.BsDate(2083, 1, 1, Calendar.TUESDAY),
            NepaliDate.toBsDate(adDate(2026, 4, 14), nepalTz)
        )
        assertEquals(
            NepaliDate.BsDate(2083, 8, 10, Calendar.THURSDAY),
            NepaliDate.toBsDate(adDate(2026, 11, 26), nepalTz)
        )
    }

    @Test
    fun monthBoundaryWith32DaysConvertsAccurately() {
        // 2083 Asar has 32 days
        val asar32 = adDate(2026, 7, 16)
        assertEquals(
            NepaliDate.BsDate(2083, 3, 32, Calendar.THURSDAY),
            NepaliDate.toBsDate(asar32, nepalTz)
        )
        val shrawan1 = adDate(2026, 7, 17)
        assertEquals(
            NepaliDate.BsDate(2083, 4, 1, Calendar.FRIDAY),
            NepaliDate.toBsDate(shrawan1, nepalTz)
        )
    }

    @Test
    fun formatsEnglishAndNepaliWithoutYear() {
        val date = adDate(2026, 11, 26)
        assertEquals(
            "Thu, 10 Mangsir",
            NepaliDate.format(date, Locale.ENGLISH, nepalTz)
        )
        assertEquals(
            "बिहीबार, १० मंसिर",
            NepaliDate.format(date, Locale("ne", "NP"), nepalTz)
        )
        assertEquals(
            "Thu, 26 Nov",
            NepaliDate.formatAd(date, Locale.ENGLISH, nepalTz)
        )
    }

    @Test
    fun outOfRangeDatesFallbackToAd() {
        val beforeRange = adDate(1943, 4, 13)
        assertNull(NepaliDate.toBsDate(beforeRange, nepalTz))
        assertEquals(
            "Tue, 13 Apr",
            NepaliDate.format(beforeRange, Locale.ENGLISH, nepalTz)
        )
    }

    @Test
    fun dstTimezoneDoesNotShiftDate() {
        val nyTz = TimeZone.getTimeZone("America/New_York")
        // Spans March/November DST transitions from anchor 1943-04-14
        val summerDate = adDate(2026, 7, 16, nyTz)
        assertEquals(
            NepaliDate.BsDate(2083, 3, 32, Calendar.THURSDAY),
            NepaliDate.toBsDate(summerDate, nyTz)
        )
    }
}
