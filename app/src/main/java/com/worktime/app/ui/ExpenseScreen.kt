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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Store
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.ui.theme.LocalExpenseInk
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.expenseCategoryLabel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ExpenseScreen(
    month: YearMonth,
    store: Store,
    today: LocalDate,
    onAdd: (LocalDate, Long, ExpenseCategory, String) -> Unit,
    onUpdate: (LocalDate, Expense) -> Unit,
    onDelete: (LocalDate, Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = ExpenseMath.entriesIn(month, store)
    val total = entries.sumOf { it.second.amountCents }
    val byCategory = ExpenseMath.byCategory(month, store)
    val dayCount = entries.map { it.first }.distinct().size
    val average = if (dayCount == 0) 0L else total / dayCount
    val isCurrentMonth = YearMonth.from(today) == month
    val defaultDate = if (isCurrentMonth) today else month.atDay(1)

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<LocalDate, Expense>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isCurrentMonth) "本月支出" else "${month.monthValue} 月支出",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "¥${ExpenseMath.formatCents(total)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = NumberFont,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(onClick = { adding = true }, shape = RoundedCornerShape(999.dp)) {
                Icon(
                    painterResource(R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("记一笔")
            }
        }
        Text(
            text = if (entries.isEmpty()) {
                "这个月还没有支出"
            } else {
                "共 ${entries.size} 笔 · 日均 ¥${ExpenseMath.formatCents(average)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("分类")
        SoftCard {
            if (byCategory.isEmpty()) {
                Text(
                    text = "记下第一笔后，这里会按分类汇总。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                byCategory.forEachIndexed { index, (category, cents) ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    CategoryBar(category = category, cents = cents, total = total)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("明细")
        SoftCard(contentPadding = 4.dp) {
            val sorted = entries.sortedByDescending { it.first }
            if (sorted.isEmpty()) {
                Text(
                    text = "点右上角「记一笔」开始记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                sorted.forEachIndexed { index, (date, expense) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                    ExpenseLine(
                        date = date,
                        expense = expense,
                        onClick = { editing = date to expense },
                    )
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    if (adding) {
        ExpenseEditDialog(
            date = defaultDate,
            initial = null,
            onSave = { cents, category, note ->
                onAdd(defaultDate, cents, category, note)
                adding = false
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }

    editing?.let { (date, expense) ->
        ExpenseEditDialog(
            date = date,
            initial = expense,
            onSave = { cents, category, note ->
                onUpdate(date, expense.copy(amountCents = cents, category = category, note = note))
                editing = null
            },
            onDelete = {
                onDelete(date, expense)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun CategoryBar(category: ExpenseCategory, cents: Long, total: Long) {
    val ink = LocalExpenseInk.current
    val color = ink.of(category)
    val fraction = if (total <= 0) 0f else (cents.toFloat() / total).coerceIn(0f, 1f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = expenseCategoryLabel(category),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "¥${ExpenseMath.formatCents(cents)}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = NumberFont,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.75f)),
            )
        }
    }
}

@Composable
private fun ExpenseLine(date: LocalDate, expense: Expense, onClick: () -> Unit) {
    val ink = LocalExpenseInk.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ink.of(expense.category)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${date.monthValue}月${date.dayOfMonth}日 · ${expenseCategoryLabel(expense.category)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (expense.note.isNotBlank()) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "¥${ExpenseMath.formatCents(expense.amountCents)}",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = NumberFont,
        )
    }
}
