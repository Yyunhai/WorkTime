package com.worktime.app.domain

import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Segment
import java.time.LocalDate
import java.time.YearMonth

/** 工时与统计的纯计算部分，不依赖 Android API，便于单测。 */
object WorkMath {

    private val WEEKDAY_NAMES = charArrayOf('一', '二', '三', '四', '五', '六', '日')

    fun parseClock(text: String): Int? {
        val parts = text.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun formatClock(hour: Int, minute: Int): String =
        "%02d:%02d".format(java.util.Locale.US, hour, minute)

    /** 当天第几分钟 → "HH:mm"，用于把数据库里的分钟还原成界面文本。 */
    fun formatMinuteOfDay(minute: Int): String =
        formatClock(minute / 60, minute % 60)

    /** 跨夜（结束不晚于开始）视为非法，返回 null。 */
    fun segmentMinutes(segment: Segment): Int? {
        val start = parseClock(segment.start) ?: return null
        val end = parseClock(segment.end) ?: return null
        return if (end <= start) null else end - start
    }

    fun segmentsMinutes(segments: List<Segment>): Int =
        segments.mapNotNull { segmentMinutes(it) }.sum()

    fun netMinutes(record: DayRecord): Int {
        if (record.status != DayStatus.work) return 0
        return (segmentsMinutes(record.segments) - record.breakMinutes).coerceAtLeast(0)
    }

    /** 单条记录的校验：返回给用户看的问题描述，null 表示合法。 */
    fun validationProblem(record: DayRecord): String? {
        if (record.status != DayStatus.work) {
            return if (record.segments.isEmpty()) null else "休息或请假日不需要时间段"
        }
        if (record.segments.isEmpty()) return "请至少添加一个时间段"
        record.segments.forEachIndexed { index, segment ->
            if (segmentMinutes(segment) == null) {
                return "第 ${index + 1} 段的结束时间必须晚于开始时间"
            }
        }
        if (netMinutes(record) <= 0) return "休息时间不小于工作时长，净工时为 0"
        return null
    }

    fun isRecordValid(record: DayRecord): Boolean = validationProblem(record) == null

    data class MonthSummary(
        val workDays: Int,
        val restDays: Int,
        val leaveDays: Int,
        val totalMinutes: Int,
    ) {
        val averageMinutes: Int get() = if (workDays == 0) 0 else totalMinutes / workDays
    }

    /** 含 [month] 在内、往前 [count] 个月的列表，按时间升序。 */
    fun lastMonths(month: YearMonth, count: Int): List<YearMonth> =
        (count - 1 downTo 0).map { month.minusMonths(it.toLong()) }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m 分"
            m == 0 -> "$h 小时"
            else -> "$h 小时 $m 分"
        }
    }

    fun formatHours(minutes: Int): String =
        "%.1f".format(java.util.Locale.US, minutes / 60.0)

    fun dayLabel(date: LocalDate): String =
        "${date.monthValue}月${date.dayOfMonth}日 周${WEEKDAY_NAMES[date.dayOfWeek.value - 1]}"

    fun shortMonthLabel(month: YearMonth): String = "${month.monthValue}月"
}
