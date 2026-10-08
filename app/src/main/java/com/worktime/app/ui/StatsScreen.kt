package com.worktime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Store
import com.worktime.app.domain.WorkMath
import com.worktime.app.ui.theme.LocalDayInk
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.statusLabel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(
    month: YearMonth,
    store: Store,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val window = WorkMath.lastMonths(month, 6)
    val totals = window.map { WorkMath.monthTotalMinutes(it, store) }
    val summary = WorkMath.summarize(month, store)
    val details = WorkMath.recordedDays(month, store).reversed()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        SectionLabel("近 6 个月")
        SoftCard {
            BarChart(window = window, totals = totals, highlight = month)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            Row {
                Metric(text = WorkMath.formatHours(summary.totalMinutes), unit = "小时", label = "本月合计", modifier = Modifier.weight(1f))
                Metric(text = "${summary.workDays}", unit = "天", label = "出勤", modifier = Modifier.weight(1f))
                Metric(
                    text = WorkMath.formatHours(WorkMath.maxDailyMinutes(month, store)),
                    unit = "小时",
                    label = "最高单日",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("${month.monthValue} 月明细")
        SoftCard {
            if (details.isEmpty()) {
                Text(
                    text = "这个月还没有记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp),
                )
            } else {
                details.forEachIndexed { index, (date, record) ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 14.dp))
                    }
                    DetailLine(
                        date = date,
                        record = record,
                        onClick = { onDayClick(date) },
                    )
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun BarChart(window: List<YearMonth>, totals: List<Int>, highlight: YearMonth) {
    val axis = (totals.maxOrNull() ?: 0).coerceAtLeast(480)
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        window.forEachIndexed { index, month ->
            val minutes = totals[index]
            val ratio = (minutes.toFloat() / axis).coerceIn(0.02f, 1f)
            val isCurrent = month == highlight
            val colors = if (isCurrent) {
                listOf(scheme.primary, scheme.tertiary)
            } else {
                listOf(scheme.primary.copy(alpha = 0.30f), scheme.tertiary.copy(alpha = 0.30f))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = if (minutes == 0) "" else WorkMath.formatHours(minutes),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = NumberFont,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((ratio * 104).dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Brush.verticalGradient(colors)),
                )
                Text(
                    text = WorkMath.shortMonthLabel(month),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Metric(text: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = NumberFont,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = " $unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailLine(date: LocalDate, record: DayRecord, onClick: () -> Unit) {
    val ink = LocalDayInk.current
    val isWork = record.status == DayStatus.work
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ink.of(record.status)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(WorkMath.dayLabel(date), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (isWork) {
                    record.segments.joinToString("  ") { "${it.start}–${it.end}" }
                } else {
                    statusLabel(record.status)
                } + if (record.note.isBlank()) "" else " · ${record.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isWork) WorkMath.formatDuration(WorkMath.netMinutes(record)) else statusLabel(record.status),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = NumberFont,
            color = ink.of(record.status),
            textAlign = TextAlign.End,
        )
    }
}
