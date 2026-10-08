package com.worktime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Store
import com.worktime.app.domain.WorkMath
import com.worktime.app.ui.theme.LocalDayInk
import com.worktime.app.ui.theme.NumberFont
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    month: YearMonth,
    store: Store,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = WorkMath.summarize(month, store)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        MonthHero(summary = summary)
        Spacer(Modifier.height(22.dp))
        WeekdayHeader()
        Spacer(Modifier.height(6.dp))
        MonthGrid(month = month, store = store, today = today, onDayClick = onDayClick)
        Spacer(Modifier.height(18.dp))
        LegendRow()
        Text(
            text = "未记录的日子不计入出勤，但计入当月天数。点任意格子开始记录。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun MonthHero(summary: WorkMath.MonthSummary) {
    Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = WorkMath.formatHours(summary.totalMinutes),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = NumberFont,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = " 小时",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "出勤 ${summary.workDays} 天 · 日均 ${WorkMath.formatHours(summary.averageMinutes)} 小时",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "休息 ${summary.restDays} 天 · 请假 ${summary.leaveDays} 天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekdayHeader() {
    val ink = LocalDayInk.current
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (index >= 5) ink.leave.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    store: Store,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val leadingBlanks = (month.atDay(1).dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayOfMonth = (row * 7 + column) - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = month.atDay(dayOfMonth)
                            DayCell(
                                date = date,
                                record = store.records[date.toString()],
                                isToday = date == today,
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    record: DayRecord?,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val ink = LocalDayInk.current
    val shape = RoundedCornerShape(14.dp)
    val accent = record?.let { ink.of(it.status) } ?: Color.Unspecified

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(shape)
            .background(
                if (record == null) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    ink.tint(record.status, 0.14f)
                },
            )
            .then(
                if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = NumberFont,
                fontWeight = if (record == null) FontWeight.Normal else FontWeight.SemiBold,
                color = if (record == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    accent
                },
            )
            val badge = dayBadge(record)
            if (badge.isNotEmpty()) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = NumberFont,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun dayBadge(record: DayRecord?): String = when (record?.status) {
    DayStatus.work -> WorkMath.formatHours(WorkMath.netMinutes(record))
    DayStatus.rest -> "休"
    DayStatus.leave -> "假"
    null -> ""
}

@Composable
private fun LegendRow() {
    val ink = LocalDayInk.current
    Row(
        modifier = Modifier.padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = ink.work, text = "工作")
        LegendDot(color = ink.rest, text = "休息")
        LegendDot(color = ink.leave, text = "请假")
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
