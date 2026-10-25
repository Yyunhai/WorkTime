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
import com.worktime.app.domain.CategoryTotal
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.ui.theme.LocalExpenseInk
import com.worktime.app.ui.theme.LocalPinkTheme
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.PinkMoneyInk
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ExpenseScreen(
    month: YearMonth,
    today: LocalDate,
    expenses: List<Expense>,
    totalCents: Long,
    categoryTotals: List<CategoryTotal>,
    categories: List<ExpenseCategory>,
    onAdd: (LocalDate, Long, Long?, String) -> Unit,
    onUpdate: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayCount = expenses.map { it.date }.distinct().size
    val average = if (dayCount == 0) 0L else totalCents / dayCount
    val isCurrentMonth = YearMonth.from(today) == month
    val defaultDate = if (isCurrentMonth) today else month.atDay(1)
    val pink = LocalPinkTheme.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }

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
                    text = "¥${ExpenseMath.formatCents(totalCents)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = NumberFont,
                    color = if (pink) PinkMoneyInk else MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = { adding = true },
                shape = RoundedCornerShape(999.dp),
                colors = primaryButtonColors(),
            ) {
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
            text = if (expenses.isEmpty()) {
                "这个月还没有支出"
            } else {
                "共 ${expenses.size} 笔 · 日均 ¥${ExpenseMath.formatCents(average)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("分类")
        SoftCard {
            if (categoryTotals.isEmpty()) {
                Text(
                    text = "记下第一笔后，这里会按分类汇总。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                categoryTotals.forEachIndexed { index, total ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    CategoryBar(name = total.name, cents = total.amountCents, total = totalCents)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("明细")
        SoftCard(contentPadding = 4.dp) {
            if (expenses.isEmpty()) {
                Text(
                    text = "点右上角「记一笔」开始记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                expenses.forEachIndexed { index, expense ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                    ExpenseLine(expense = expense, onClick = { editing = expense })
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    if (adding) {
        ExpenseEditDialog(
            date = defaultDate,
            initial = null,
            categories = categories,
            onSave = { cents, categoryId, note ->
                onAdd(defaultDate, cents, categoryId, note)
                adding = false
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }

    editing?.let { expense ->
        ExpenseEditDialog(
            date = expense.date,
            initial = expense,
            categories = categories,
            onSave = { cents, categoryId, note ->
                onUpdate(
                    expense.copy(amountCents = cents, categoryId = categoryId, note = note),
                )
                editing = null
            },
            onDelete = {
                onDelete(expense)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun CategoryBar(name: String, cents: Long, total: Long) {
    val ink = LocalExpenseInk.current
    val pink = LocalPinkTheme.current
    val color = ink.of(name)
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
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "¥${ExpenseMath.formatCents(cents)}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = NumberFont,
                color = if (pink) PinkMoneyInk else MaterialTheme.colorScheme.onSurface,
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
private fun ExpenseLine(expense: Expense, onClick: () -> Unit) {
    val ink = LocalExpenseInk.current
    val pink = LocalPinkTheme.current
    val categoryName = expense.categoryName ?: "未分类"
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
                .background(ink.of(categoryName)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${expense.date.monthValue}月${expense.date.dayOfMonth}日 · $categoryName",
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
            color = if (pink) PinkMoneyInk else MaterialTheme.colorScheme.onSurface,
        )
    }
}
