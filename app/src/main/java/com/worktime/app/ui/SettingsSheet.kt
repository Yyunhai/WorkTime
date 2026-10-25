package com.worktime.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worktime.app.data.ThemeMode
import com.worktime.app.ui.theme.themeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themeMode: ThemeMode,
    launchArtEnabled: Boolean,
    busy: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onLaunchArt: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var askClear by remember { mutableStateOf(false) }

    // 直接完整展开；内容再长也能在面板里滚动，不会被截断在半屏。
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(18.dp))
            SectionLabel("外观")
            SegmentedPills(
                options = ThemeMode.entries,
                selected = themeMode,
                labelOf = ::themeLabel,
                onSelect = onTheme,
            )
            Spacer(Modifier.height(12.dp))
            SoftCard(contentPadding = 6.dp) {
                SwitchRow(
                    title = "随机开屏图片",
                    description = "打开应用时随机显示一张全屏图片，短暂停留后淡出；关闭后直接进入主界面",
                    checked = launchArtEnabled,
                    onCheckedChange = onLaunchArt,
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("数据")
            SoftCard(contentPadding = 4.dp) {
                ActionRow(
                    title = "导出备份",
                    description = "生成一个 JSON 文件，自己保存到手机上",
                    enabled = !busy,
                    onClick = onExport,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                )
                ActionRow(
                    title = "导入备份",
                    description = "选择备份文件，按日期合并进当前记录",
                    enabled = !busy,
                    onClick = onImport,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                )
                ActionRow(
                    title = "清空全部数据",
                    description = "工作记录和支出一起删除，导出过的备份不受影响",
                    enabled = !busy,
                    danger = true,
                    onClick = { askClear = true },
                )
            }

            if (busy) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在读写文件…", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "数据保存在本机的数据库和设置文件里，卸载应用或清除数据会一起丢失，" +
                    "所以重装前请先导出备份。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("确认清空？") },
            text = { Text("全部工作记录和支出都会被删除，且无法恢复。建议先导出备份。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        askClear = false
                        onClear()
                    },
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { askClear = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    description: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
