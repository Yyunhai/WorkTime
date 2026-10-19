package com.worktime.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.worktime.app.data.Backup
import com.worktime.app.data.DayRecord
import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Store
import com.worktime.app.data.StoreRepository
import com.worktime.app.data.ThemeMode
import com.worktime.app.domain.ExpenseMath
import com.worktime.app.domain.WorkMath
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: StoreRepository,
    private val appContext: Context,
) : ViewModel() {

    data class PendingImport(val plan: Backup.ImportPlan, val incoming: Store)

    val store: StateFlow<Store> = repository.store
    val loaded: StateFlow<Boolean> = repository.loaded
    val loadError: StateFlow<StoreRepository.LoadError?> = repository.loadError

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun saveDay(date: LocalDate, record: DayRecord): Boolean {
        val key = date.toString()
        return guardedUpdate(
            transform = { it.copy(records = it.records + (key to record)) },
            success = "已保存 $key",
            failure = "写入失败，请重试",
        )
    }

    suspend fun deleteDay(date: LocalDate): Boolean {
        val key = date.toString()
        return guardedUpdate(
            transform = { it.copy(records = it.records - key) },
            success = "已删除 $key 的记录",
            failure = "写入失败，请重试",
        )
    }

    suspend fun addExpense(
        date: LocalDate,
        amountCents: Long,
        category: ExpenseCategory,
        note: String,
    ): Boolean {
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amountCents = amountCents,
            category = category,
            note = note.trim(),
        )
        return mutateExpenses(
            date = date,
            success = "已记 ¥${ExpenseMath.formatCents(amountCents)}",
            failure = "写入失败，请重试",
        ) { it + expense }
    }

    suspend fun updateExpense(date: LocalDate, expense: Expense): Boolean {
        val normalized = expense.copy(note = expense.note.trim())
        return mutateExpenses(
            date = date,
            success = "已更新支出",
            failure = "写入失败，请重试",
        ) { list -> list.map { if (it.id == normalized.id) normalized else it } }
    }

    suspend fun deleteExpense(date: LocalDate, expenseId: String): Boolean =
        mutateExpenses(
            date = date,
            success = "已删除支出",
            failure = "写入失败，请重试",
        ) { list -> list.filterNot { it.id == expenseId } }

    private suspend fun mutateExpenses(
        date: LocalDate,
        success: String,
        failure: String,
        transform: (List<Expense>) -> List<Expense>,
    ): Boolean {
        val key = date.toString()
        return guardedUpdate(
            transform = { store ->
                val next = transform(store.expenses[key].orEmpty())
                store.copy(
                    expenses = if (next.isEmpty()) {
                        store.expenses - key
                    } else {
                        store.expenses + (key to next)
                    },
                )
            },
            success = success,
            failure = failure,
        )
    }

    fun setTheme(mode: ThemeMode) {
        scope.launch {
            repository.update { it.copy(settings = it.settings.copy(theme = mode)) }
        }
    }

    fun setLaunchArt(enabled: Boolean) {
        scope.launch {
            repository.update { it.copy(settings = it.settings.copy(launchArt = enabled)) }
        }
    }

    fun exportTo(uri: Uri) {
        scope.launch {
            _busy.value = true
            val ok = Backup.export(appContext, store.value, uri)
            _busy.value = false
            _messages.tryEmit(if (ok) "已导出备份" else "导出失败")
        }
    }

    fun previewImport(uri: Uri) {
        scope.launch {
            _busy.value = true
            val raw = Backup.read(appContext, uri)
            _busy.value = false
            if (raw == null) {
                _messages.tryEmit("文件无法读取")
                return@launch
            }
            // 导入前先过滤掉无法参与统计的记录/支出，避免坏数据混进本机。
            val incoming = ExpenseMath.sanitize(WorkMath.sanitize(raw))
            val droppedDays = raw.records.size - incoming.records.size
            val droppedExpenses = raw.expenses.values.sumOf { it.size } -
                incoming.expenses.values.sumOf { it.size }
            val skipped = listOfNotNull(
                droppedDays.takeIf { it > 0 }?.let { "$it 天无效记录" },
                droppedExpenses.takeIf { it > 0 }?.let { "$it 笔无效支出" },
            )
            if (skipped.isNotEmpty()) _messages.tryEmit("已忽略 ${skipped.joinToString("、")}")
            _pendingImport.value = PendingImport(Backup.plan(store.value, incoming), incoming)
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun applyImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        scope.launch {
            val ok = repository.replace(Backup.merge(store.value, pending.incoming))
            _messages.tryEmit(if (ok) "已导入 ${pending.plan.affectedDays} 天" else "导入失败")
        }
    }

    fun clearAll() {
        scope.launch {
            val ok = repository.replace(Store(settings = store.value.settings))
            _messages.tryEmit(if (ok) "已清空全部记录" else "清空失败")
        }
    }

    /** 加载完成前拒绝写入，避免用空的初始 Store 覆盖磁盘上的旧记录。 */
    private suspend fun guardedUpdate(
        transform: (Store) -> Store,
        success: String,
        failure: String,
    ): Boolean {
        if (!loaded.value) {
            _messages.tryEmit("数据还在加载，请稍候")
            return false
        }
        val ok = repository.update(transform)
        _messages.tryEmit(if (ok) success else failure)
        return ok
    }

    companion object {
        fun factory(repository: StoreRepository, context: Context) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(repository, context.applicationContext) as T
            }
    }
}
