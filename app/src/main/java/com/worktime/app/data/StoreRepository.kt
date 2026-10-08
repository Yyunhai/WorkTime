package com.worktime.app.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** 全量记录的唯一内存态来源；落盘为应用私有目录下的单个 JSON 文件。 */
class StoreRepository(context: Context) {

    private val dir = File(context.filesDir, "worktime")
    private val file = File(dir, "data.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _store = MutableStateFlow(Store())
    val store: StateFlow<Store> = _store.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun startLoad() {
        scope.launch {
            val loadedStore = read()
            if (loadedStore != null) _store.value = loadedStore
            _loaded.value = true
        }
    }

    /** 应用变换并立刻落盘。返回 false 表示写入失败，内存态保持变换前的值。 */
    fun update(transform: (Store) -> Store): Boolean {
        val current = _store.value
        val next = transform(current)
        if (next == current) return true
        _store.value = next
        if (!write(next)) {
            _store.value = current
            return false
        }
        return true
    }

    /** 供导入时整体合并使用。 */
    fun replace(next: Store): Boolean = update { next }

    private fun read(): Store? = try {
        if (!file.isFile) null else json.decodeFromString(Store.serializer(), file.readText())
    } catch (_: Exception) {
        null
    }

    private fun write(value: Store): Boolean {
        val tmp = File(dir, "data.json.tmp")
        return try {
            if (!dir.exists() && !dir.mkdirs()) return false
            tmp.writeText(json.encodeToString(Store.serializer(), value))
            if (file.exists() && !file.delete()) return false
            tmp.renameTo(file)
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }
}
