package com.worktime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worktime.app.R
import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.domain.WorkMath
import com.worktime.app.ui.theme.LocalExpenseInk
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.expenseCategoryLabel
import java.time.LocalDate

/** 新增/编辑单笔支出的卡片弹窗。用 Dialog 而不是 BottomSheet，方便叠在日编辑面板之上。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditDialog(
    date: LocalDate,
    initial: Expense?,
    onSave: (amountCents: Long, category: ExpenseCategory, note: String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var amountText by remember {
        mutableStateOf(initial?.let { ExpenseMath.formatPlain(it.amountCents) } ?: "")
    }
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.food) }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    val cents = ExpenseMath.parseAmountToCents(amountText)
    val amountInvalid = amountText.isNotBlank() && cents == null
    val ink = LocalExpenseInk.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 16.dp),
            ) {
                Text(
                    text = if (initial == null) "记一笔" else "编辑支出",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = WorkMath.dayLabel(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() || it == '.' }.take(12)
                    },
                    label = { Text("金额") },
                    prefix = { Text("¥", fontFamily = NumberFont) },
                    singleLine = true,
                    isError = amountInvalid,
                    supportingText = if (amountInvalid) {
                        { Text("请输入大于 0 的金额，最多两位小数") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = NumberFont),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "分类",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpenseCategory.entries.forEach { option ->
                        Pill(
                            text = expenseCategoryLabel(option),
                            selected = option == category,
                            onClick = { category = option },
                            accent = ink.of(option),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(100) },
                    label = { Text("备注（可不填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_trash),
                                contentDescription = "删除这笔支出",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { cents?.let { onSave(it, category, note) } },
                        enabled = cents != null,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(if (initial == null) "记下" else "保存")
                    }
                }
            }
        }
    }
}
