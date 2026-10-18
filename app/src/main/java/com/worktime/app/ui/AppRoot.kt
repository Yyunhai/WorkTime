package com.worktime.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.worktime.app.AppViewModel
import com.worktime.app.R
import com.worktime.app.ui.theme.NumberFont
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: AppViewModel) {
    val store by vm.store.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val pendingImport by vm.pendingImport.collectAsState()
    val busy by vm.busy.collectAsState()

    WorkTimeTheme(mode = store.settings.theme) {
        val today = remember { LocalDate.now() }
        var shownMonth by remember { mutableStateOf(YearMonth.from(today)) }
        var editingDate by remember { mutableStateOf<LocalDate?>(null) }
        var showSettings by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()

        LaunchedEffect(vm) {
            vm.messages.collect { snackbarHostState.showSnackbar(it) }
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri -> uri?.let { vm.exportTo(it) } }

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let { vm.previewImport(it) } }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    AppTopBar(
                        month = shownMonth,
                        isCurrentMonth = shownMonth == YearMonth.from(today),
                        page = pagerState.currentPage,
                        onPrevious = { shownMonth = shownMonth.minusMonths(1) },
                        onNext = { shownMonth = shownMonth.plusMonths(1) },
                        onBackToCurrentMonth = { shownMonth = YearMonth.from(today) },
                        onSettings = { showSettings = true },
                        onSelectPage = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (pagerState.currentPage == 0) {
                        ExtendedFloatingActionButton(
                            onClick = { editingDate = today },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
                            text = { Text("记录今天") },
                        )
                    }
                },
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) { page ->
                    when (page) {
                        0 -> CalendarScreen(
                            month = shownMonth,
                            store = store,
                            today = today,
                            onDayClick = { editingDate = it },
                        )

                        else -> StatsScreen(
                            month = shownMonth,
                            store = store,
                            onDayClick = { editingDate = it },
                        )
                    }
                }
            }

            editingDate?.let { date ->
                DayEditSheet(
                    date = date,
                    record = store.records[date.toString()],
                    onSave = { record ->
                        vm.saveDay(date, record)
                        editingDate = null
                    },
                    onDelete = {
                        vm.deleteDay(date)
                        editingDate = null
                    },
                    onDismiss = { editingDate = null },
                )
            }

            if (showSettings) {
                SettingsSheet(
                    themeMode = store.settings.theme,
                    launchArtEnabled = store.settings.launchArt,
                    busy = busy,
                    onTheme = vm::setTheme,
                    onLaunchArt = vm::setLaunchArt,
                    onExport = { exportLauncher.launch(suggestedFileName(today)) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    onClear = { vm.clearAll() },
                    onDismiss = { showSettings = false },
                )
            }

            pendingImport?.let { pending ->
                AlertDialog(
                    onDismissRequest = vm::cancelImport,
                    title = { Text("确认导入") },
                    text = {
                        Text(
                            "备份中有 ${pending.plan.affectedDays} 天记录：" +
                                "新增 ${pending.plan.adding} 天，覆盖 ${pending.plan.overwriting} 天。" +
                                "覆盖会用备份里的内容替换本机同名日期的记录。",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = vm::applyImport) { Text("导入") }
                    },
                    dismissButton = {
                        TextButton(onClick = vm::cancelImport) { Text("取消") }
                    },
                )
            }

            LaunchArt(enabled = loaded && store.settings.launchArt)
        }
    }
}

private fun suggestedFileName(today: LocalDate): String =
    "worktime-%04d%02d.json".format(java.util.Locale.US, today.year, today.monthValue)

@Composable
private fun AppTopBar(
    month: YearMonth,
    isCurrentMonth: Boolean,
    page: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onSettings: () -> Unit,
    onSelectPage: (Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp),
            ) {
                Text(
                    text = "工作记录",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                SegmentedPills(
                    options = listOf(0, 1),
                    selected = page,
                    labelOf = { if (it == 0) "月历" else "统计" },
                    onSelect = onSelectPage,
                    modifier = Modifier.width(150.dp),
                )
                TonalIconButton(onClick = onSettings, icon = R.drawable.ic_more, description = "设置")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            ) {
                TonalIconButton(onClick = onPrevious, icon = R.drawable.ic_chevron_left, description = "上一月")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBackToCurrentMonth)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${month.year} 年 ${month.monthValue} 月",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NumberFont,
                    )
                    Text(
                        text = if (isCurrentMonth) "本月" else "点击回到本月",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                TonalIconButton(onClick = onNext, icon = R.drawable.ic_chevron_right, description = "下一月")
            }
        }
    }
}
