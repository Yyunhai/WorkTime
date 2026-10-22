package com.worktime.app.domain

import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Segment
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
    fun minuteOfDayFormatting() {
        assertEquals("00:00", WorkMath.formatMinuteOfDay(0))
        assertEquals("11:00", WorkMath.formatMinuteOfDay(660))
        assertEquals("23:59", WorkMath.formatMinuteOfDay(1439))
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
