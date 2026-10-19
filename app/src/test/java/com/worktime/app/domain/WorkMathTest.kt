package com.worktime.app.domain

import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Segment
import com.worktime.app.data.Store
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkMathTest {

    private fun work(vararg pairs: Pair<String, String>, breakMinutes: Int = 0) =
        DayRecord(
            status = DayStatus.work,
            segments = pairs.map { Segment(it.first, it.second) },
            breakMinutes = breakMinutes,
        )

    @Test
    fun multiSegmentsMinusBreak() {
        val record = work("09:10" to "12:00", "13:30" to "18:05", breakMinutes = 0)
        assertEquals(170 + 275, WorkMath.netMinutes(record))
    }

    @Test
    fun breakNeverGoesNegative() {
        assertEquals(0, WorkMath.netMinutes(work("09:00" to "10:00", breakMinutes = 120)))
    }

    @Test
    fun overnightSegmentRejected() {
        val record = work("22:00" to "02:00")
        assertNull(WorkMath.segmentMinutes(record.segments.first()))
        assertFalse(WorkMath.isRecordValid(record))
    }

    @Test
    fun nonWorkDaysHaveZeroHours() {
        assertEquals(0, WorkMath.netMinutes(DayRecord(status = DayStatus.rest)))
        assertEquals(0, WorkMath.netMinutes(DayRecord(status = DayStatus.leave)))
    }

    @Test
    fun emptySegmentListIsInvalid() {
        assertFalse(WorkMath.isRecordValid(DayRecord(status = DayStatus.work)))
        assertTrue(WorkMath.isRecordValid(DayRecord(status = DayStatus.rest)))
    }

    @Test
    fun clockParsingBounds() {
        assertEquals(0, WorkMath.parseClock("00:00"))
        assertEquals(1439, WorkMath.parseClock("23:59"))
        assertNull(WorkMath.parseClock("24:00"))
        assertNull(WorkMath.parseClock("9:65"))
        assertNull(WorkMath.parseClock("0900"))
    }

    @Test
    fun summaryCountsWorkDaysAndAverage() {
        val month = YearMonth.of(2026, 10)
        val store = Store(
            records = mapOf(
                "2026-10-08" to work("09:00" to "18:00", breakMinutes = 60),
                "2026-10-09" to work("09:00" to "13:00"),
                "2026-10-10" to DayRecord(status = DayStatus.rest),
                "2026-10-11" to DayRecord(status = DayStatus.leave),
            ),
        )
        val summary = WorkMath.summarize(month, store)
        assertEquals(2, summary.workDays)
        assertEquals(1, summary.restDays)
        assertEquals(1, summary.leaveDays)
        assertEquals(480 + 240, summary.totalMinutes)
        assertEquals(360, summary.averageMinutes)
        assertEquals(4, summary.recordedDays)
    }

    @Test
    fun recordedDaysAreSortedAscending() {
        val month = YearMonth.of(2026, 10)
        val store = Store(
            records = mapOf(
                "2026-10-20" to DayRecord(status = DayStatus.rest),
                "2026-10-03" to DayRecord(status = DayStatus.leave),
                "2026-09-01" to DayRecord(status = DayStatus.rest),
            ),
        )
        val days = WorkMath.recordedDays(month, store)
        assertEquals(2, days.size)
        assertEquals("2026-10-03", days.first().first.toString())
    }

    @Test
    fun maxDailyMinutesPicksBestDayAndIgnoresOtherMonths() {
        val month = YearMonth.of(2026, 10)
        val store = Store(
            records = mapOf(
                "2026-10-08" to work("09:00" to "18:00", breakMinutes = 60),
                "2026-10-09" to work("09:00" to "21:30", breakMinutes = 30),
                "2026-10-10" to DayRecord(status = DayStatus.rest),
                "2026-11-01" to work("00:00" to "23:00"),
            ),
        )
        assertEquals(720, WorkMath.maxDailyMinutes(month, store))
        assertEquals(0, WorkMath.maxDailyMinutes(YearMonth.of(2026, 12), store))
    }

    @Test
    fun lastMonthsWindow() {
        val months = WorkMath.lastMonths(YearMonth.of(2026, 3), 3)
        assertEquals(
            listOf("2026-01", "2026-02", "2026-03"),
            months.map { it.toString() },
        )
    }

    @Test
    fun nonWorkDayWithSegmentsIsInvalid() {
        assertFalse(
            WorkMath.isRecordValid(
                DayRecord(status = DayStatus.rest, segments = listOf(Segment("09:00", "10:00"))),
            ),
        )
    }

    @Test
    fun zeroNetWorkDayIsNotAttendance() {
        val month = YearMonth.of(2026, 10)
        val store = Store(
            records = mapOf("2026-10-08" to work("09:00" to "10:00", breakMinutes = 60)),
        )

        val summary = WorkMath.summarize(month, store)

        assertEquals(0, summary.workDays)
        assertEquals(0, summary.totalMinutes)
        assertEquals(0, summary.averageMinutes)
    }

    @Test
    fun sanitizeDropsRecordsThatCannotBeCounted() {
        val store = Store(
            records = mapOf(
                "2026-10-08" to work("09:00" to "18:00"),
                "2026-10-09" to work("22:00" to "02:00"),
                "2026-10-10" to DayRecord(status = DayStatus.rest),
                "2026-10-11" to DayRecord(
                    status = DayStatus.leave,
                    segments = listOf(Segment("09:00", "10:00")),
                ),
            ),
        )

        assertEquals(setOf("2026-10-08", "2026-10-10"), WorkMath.sanitize(store).records.keys)
    }

    @Test
    fun validationProblemExplainsTheFirstIssue() {
        assertEquals(
            "请至少添加一个时间段",
            WorkMath.validationProblem(DayRecord(status = DayStatus.work)),
        )
        assertEquals(
            "第 2 段的结束时间必须晚于开始时间",
            WorkMath.validationProblem(work("09:00" to "10:00", "12:00" to "11:00")),
        )
        assertNull(WorkMath.validationProblem(work("09:00" to "18:00")))
        assertNull(WorkMath.validationProblem(DayRecord(status = DayStatus.rest)))
    }

    @Test
    fun durationFormatting() {
        assertEquals("45 分", WorkMath.formatDuration(45))
        assertEquals("8 小时", WorkMath.formatDuration(480))
        assertEquals("7 小时 55 分", WorkMath.formatDuration(475))
        assertEquals("7.9", WorkMath.formatHours(475))
    }
}
