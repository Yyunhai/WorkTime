package com.worktime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worktime.app.R
import com.worktime.app.domain.WorkMath
import com.worktime.app.ui.theme.NumberFont
import kotlin.math.abs
import kotlinx.coroutines.launch

private val WheelItemHeight = 38.dp
private const val VisibleRows = 5
private const val Loops = 3

/**
 * 上下滑动选时间的滚轮卡片：大字号读数 + 两列吸附滚轮 + 上下渐隐遮罩。
 * 刻意不用 AlertDialog 的原生排版（标题+小字列表+两个文字按钮），那套观感很系统工具。
 */
@Composable
fun ClockPickerDialog(
    value: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parsed = WorkMath.parseClock(value) ?: (9 * 60)
    var hour by remember { mutableIntStateOf(parsed / 60) }
    var minute by remember { mutableIntStateOf(parsed % 60) }
    val cardBackground = MaterialTheme.colorScheme.surfaceContainerHigh

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = cardBackground,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = "取消",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = WorkMath.formatClock(hour, minute),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = NumberFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeWheel(
                        cycle = 24,
                        initial = hour,
                        caption = "时",
                        overlay = cardBackground,
                        onSelect = { hour = it },
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = NumberFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TimeWheel(
                        cycle = 60,
                        initial = minute,
                        caption = "分",
                        overlay = cardBackground,
                        onSelect = { minute = it },
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { onConfirm(WorkMath.formatClock(hour, minute)) },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp),
                        colors = primaryButtonColors(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("确定", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeWheel(
    cycle: Int,
    initial: Int,
    caption: String,
    overlay: Color,
    onSelect: (Int) -> Unit,
) {
    val slots = cycle * Loops
    val startSlot = Loops / 2 * cycle + initial
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startSlot - VisibleRows / 2)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val snapped by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - middle) }
                ?.index ?: startSlot
        }
    }

    LaunchedEffect(snapped) {
        onSelect(((snapped % cycle) + cycle) % cycle)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(78.dp)
                .height(WheelItemHeight * VisibleRows),
        ) {
            // 选中行的底条：比文字先画，保证数字压在上面
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(vertical = WheelItemHeight * (VisibleRows / 2)),
            ) {
                items(count = slots) { slot ->
                    val distance = abs(slot - snapped)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WheelItemHeight)
                            .clickable { scope.launch { listState.animateScrollToItem(slot - VisibleRows / 2) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (((slot % cycle) + cycle) % cycle).toString().padStart(2, '0'),
                            style = if (distance == 0) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontFamily = NumberFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = when (distance) {
                                    0 -> 1f
                                    1 -> 0.45f
                                    else -> 0.16f
                                },
                            ),
                        )
                    }
                }
            }
            // 上下渐隐：滚轮感主要靠这两层遮罩
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to overlay,
                            0.32f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1f to overlay,
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
