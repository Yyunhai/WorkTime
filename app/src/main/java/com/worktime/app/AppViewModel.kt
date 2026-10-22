package com.worktime.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.worktime.app.data.Backup
import com.worktime.app.data.BackupFile
import com.worktime.app.data.DayRecord
import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Settings
import com.worktime.app.data.ThemeMode
import com.worktime.app.data.WorkRepository
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.domain.MonthData
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: WorkRepository,
) : ViewModel() {

    data class PendingImport(val plan: Backup.ImportPlan, val file: BackupFile)

    val loaded: StateFlow<Boolean> = repository.loaded
    val loadError: StateFlow<String?> = repository.loadError
    val settings: StateFlow<Settings> = repository.settings

    val categories: StateFlow<List<ExpenseCategory>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthData: StateFlow<MonthData> = month
        .flatMapLatest { repository.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthData.Empty)

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setMonth(value: YearMonth) {
        month.value = value
    }

    fun saveDay(date: LocalDate, record: DayRecord, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.saveDay(date, record)
            _messages.tryEmit(if (ok) "已保存 $date" else "写入失败，请重试")
            onResult(ok)
        }
    }

    fun deleteDay(date: LocalDate, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.deleteDay(date)
            _messages.tryEmit(if (ok) "已删除 $date 的记录" else "写入失败，请重试")
            onResult(ok)
        }
    }

    fun addExpense(
        date: LocalDate,
        amountCents: Long,
        categoryId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            val ok = repository.addExpense(date, amountCents, categoryId, note)
            _messages.tryEmit(
                if (ok) "已记 ¥${ExpenseMath.formatCents(amountCents)}" else "写入失败，请重试",
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            val ok = repository.updateExpense(expense)
            _messages.tryEmit(if (ok) "已更新支出" else "写入失败，请重试")
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val ok = repository.deleteExpense(expenseId)
            _messages.tryEmit(if (ok) "已删除支出" else "写入失败，请重试")
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { repository.setTheme(mode) }
    }

    fun setLaunchArt(enabled: Boolean) {
        viewModelScope.launch { repository.setLaunchArt(enabled) }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val ok = repository.exportTo(uri)
            _busy.value = false
            _messages.tryEmit(if (ok) "已导出备份" else "导出失败")
        }
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val preview = repository.previewImport(uri)
            _busy.value = false
            if (preview == null) {
                _messages.tryEmit("文件无法读取")
            } else {
                _pendingImport.value = PendingImport(preview.plan, preview.file)
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun applyImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            val ok = repository.applyImport(pending.file)
            _messages.tryEmit(
                if (ok) "已导入 ${pending.plan.affectedDays} 天" else "导入失败",
            )
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val ok = repository.clearAll()
            _messages.tryEmit(if (ok) "已清空全部数据" else "清空失败")
        }
    }

    companion object {
        fun factory(repository: WorkRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(repository) as T
            }
    }
}
