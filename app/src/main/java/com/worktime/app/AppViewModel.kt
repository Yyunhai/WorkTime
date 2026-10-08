package com.worktime.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.worktime.app.data.Backup
import com.worktime.app.data.DayRecord
import com.worktime.app.data.Store
import com.worktime.app.data.StoreRepository
import com.worktime.app.data.ThemeMode
import java.time.LocalDate
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
import android.net.Uri

class AppViewModel(
    private val repository: StoreRepository,
    private val appContext: Context,
) : ViewModel() {

    data class PendingImport(val plan: Backup.ImportPlan, val incoming: Store)

    val store: StateFlow<Store> = repository.store
    val loaded: StateFlow<Boolean> = repository.loaded

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

    fun saveDay(date: LocalDate, record: DayRecord) {
        val key = date.toString()
        val ok = repository.update { it.copy(records = it.records + (key to record)) }
        _messages.tryEmit(if (ok) "已保存 $key" else "写入失败，请重试")
    }

    fun deleteDay(date: LocalDate) {
        val key = date.toString()
        val ok = repository.update { it.copy(records = it.records - key) }
        _messages.tryEmit(if (ok) "已删除 $key 的记录" else "写入失败，请重试")
    }

    fun setTheme(mode: ThemeMode) {
        repository.update { it.copy(settings = it.settings.copy(theme = mode)) }
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
            val incoming = Backup.read(appContext, uri)
            _busy.value = false
            if (incoming == null) {
                _messages.tryEmit("文件无法读取")
            } else {
                _pendingImport.value = PendingImport(Backup.plan(store.value, incoming), incoming)
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun applyImport() {
        val pending = _pendingImport.value ?: return
        val ok = repository.replace(Backup.merge(store.value, pending.incoming))
        _pendingImport.value = null
        _messages.tryEmit(
            if (ok) "已导入 ${pending.plan.affectedDays} 天" else "导入失败"
        )
    }

    fun clearAll() {
        val ok = repository.replace(Store(settings = store.value.settings))
        _messages.tryEmit(if (ok) "已清空全部记录" else "清空失败")
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
