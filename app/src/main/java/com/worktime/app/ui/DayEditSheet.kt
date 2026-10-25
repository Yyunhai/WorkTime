package com.worktime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.data.DayRecord
import com.worktime.app.data.DayStatus
import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Segment
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.domain.WorkMath
import com.worktime.app.ui.theme.LocalDayInk
import com.worktime.app.ui.theme.LocalExpenseInk
import com.worktime.app.ui.theme.LocalPinkTheme
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.PinkMoneyInk
import com.worktime.app.ui.theme.statusLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditSheet(
    date: LocalDate,
    record: DayRecord?,
    expenses: List<Expense>,
    categories: List<ExpenseCategory>,
    onAddExpense: (amountCents: Long, categoryId: Long?, note: String) -> Unit,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onSave: (DayRecord) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pink = LocalPinkTheme.current
    var status by remember(date) { mutableStateOf(record?.status ?: DayStatus.work) }
    val segments = remember(date) {
        mutableStateListOf<Segment>().apply {
            addAll(record?.segments ?: listOf(Segment("09:00", "18:00")))
        }
    }
    var breakText by remember(date) { mutableStateOf((record?.breakMinutes ?: 0).toString()) }
    var note by remember(date) { mutableStateOf(record?.note ?: "") }
    var addingExpense by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    val breakMinutes = breakText.toIntOrNull()?.coerceIn(0, 720) ?: 0
    val draft = DayRecord(
        status = status,
        segments = if (status == DayStatus.work) segments.toList() else emptyList(),
        breakMinutes = breakMinutes,
        note = note.trim(),
    )
    val problem = WorkMath.validationProblem(draft)
    val ink = LocalDayInk.current

    // 直接展开到完整高度：默认的半展开会让表单只露一半，还要手动往上拉。
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(WorkMath.dayLabel(date), style = MaterialTheme.typography.titleLarge)
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NumberFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))
            SegmentedPills(
                options = DayStatus.entries,
                selected = status,
                labelOf = ::statusLabel,
                onSelect = { status = it },
            )

            if (status == DayStatus.work) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("时间段", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { segments.add(Segment("13:30", "18:00")) }) {
                        Text("加一段")
                    }
                }
                SoftCard(contentPadding = 6.dp) {
                    segments.forEachIndexed { index, segment ->
                        SegmentRow(
                            accent = ink.work,
                            index = index,
                            segment = segment,
                            removable = segments.size > 1,
                            onChange = { segments[index] = it },
                            onRemove = { segments.removeAt(index) },
                        )
                        if (index < segments.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 14.dp, end = 14.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("休息")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("0", "15", "30", "60").forEach { preset ->
                        Pill(
                            text = if (preset == "0") "无" else "$preset 分",
                            selected = breakMinutes == preset.toInt(),
                            onClick = { breakText = preset },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = breakText,
                        onValueChange = { breakText = it.filter(Char::isDigit).take(3) },
                        label = { Text("休息分钟") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    NetDuration(minutes = WorkMath.netMinutes(draft))
                }
            } else {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (status == DayStatus.rest) "休息日不用填时间。" else "请假日不用填时间。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("备注（可不填）") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (problem != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("支出", modifier = Modifier.weight(1f))
                TextButton(onClick = { addingExpense = true }) { Text("＋ 记一笔") }
            }
            if (expenses.isEmpty()) {
                Text(
                    text = "这一天还没有支出。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            } else {
                SoftCard(contentPadding = 4.dp) {
                    expenses.forEachIndexed { index, expense ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                        DayExpenseLine(expense = expense, onClick = { editingExpense = expense })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "当日合计 ¥${ExpenseMath.formatCents(expenses.sumOf { it.amountCents })}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pink) PinkMoneyInk else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (record != null) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_trash),
                            contentDescription = "删除记录",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onSave(draft) },
                    enabled = problem == null,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = primaryButtonColors(),
                ) {
                    Text(if (record == null) "保存" else "更新")
                }
            }
        }
    }

    if (addingExpense) {
        ExpenseEditDialog(
            date = date,
            initial = null,
            categories = categories,
            onSave = { cents, categoryId, expenseNote ->
                onAddExpense(cents, categoryId, expenseNote)
                addingExpense = false
            },
            onDelete = null,
            onDismiss = { addingExpense = false },
        )
    }

    editingExpense?.let { expense ->
        ExpenseEditDialog(
            date = date,
            initial = expense,
            categories = categories,
            onSave = { cents, categoryId, expenseNote ->
                onUpdateExpense(
                    expense.copy(amountCents = cents, categoryId = categoryId, note = expenseNote),
                )
                editingExpense = null
            },
            onDelete = {
                onDeleteExpense(expense)
                editingExpense = null
            },
            onDismiss = { editingExpense = null },
        )
    }
}

@Composable
private fun DayExpenseLine(expense: Expense, onClick: () -> Unit) {
    val ink = LocalExpenseInk.current
    val pink = LocalPinkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ink.of(expense.categoryName)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = expense.categoryName ?: "未分类",
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
        Text(
            text = "¥${ExpenseMath.formatCents(expense.amountCents)}",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = NumberFont,
            color = if (pink) PinkMoneyInk else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NetDuration(minutes: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "净工时",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = WorkMath.formatDuration(minutes),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = NumberFont,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SegmentRow(
    accent: Color,
    index: Int,
    segment: Segment,
    removable: Boolean,
    onChange: (Segment) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = NumberFont,
                color = accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        ClockField(segment.start) { onChange(segment.withStart(it)) }
        Text(
            text = "–",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        ClockField(segment.end) { onChange(segment.withEnd(it)) }
        if (segment.endNextDay) {
            Text(
                text = "次日",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (removable) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "删除这一段",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
    }
}

private fun Segment.withStart(value: String): Segment {
    val startMinute = WorkMath.parseClock(value)
    val endMinute = WorkMath.parseClock(end)
    return copy(
        start = value,
        endNextDay = startMinute != null && endMinute != null && endMinute <= startMinute,
    )
}

private fun Segment.withEnd(value: String): Segment {
    val startMinute = WorkMath.parseClock(start)
    val endMinute = WorkMath.parseClock(value)
    return copy(
        end = value,
        endNextDay = startMinute != null && endMinute != null && endMinute <= startMinute,
    )
}

@Composable
private fun ClockField(value: String, onChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = NumberFont,
        )
    }
    if (showPicker) {
        ClockPickerDialog(
            value = value,
            label = "选择时间",
            onDismiss = { showPicker = false },
            onConfirm = {
                onChange(it)
                showPicker = false
            },
        )
    }
}


