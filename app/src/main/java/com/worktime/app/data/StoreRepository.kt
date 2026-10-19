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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 全量记录的唯一内存态来源；落盘为应用私有目录下的单个 JSON 文件。 */
class StoreRepository(context: Context) {

    private val storeFile = StoreFile(
        file = File(File(context.filesDir, "worktime"), "data.json"),
        json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 读盘与写盘串行化，避免加载和保存交错。 */
    private val fileMutex = Mutex()

    private val _store = MutableStateFlow(Store())
    val store: StateFlow<Store> = _store.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _loadError = MutableStateFlow<LoadError?>(null)
    val loadError: StateFlow<LoadError?> = _loadError.asStateFlow()

    /** 启动读盘的异常情况；[Corrupt] 表示坏文件已留档，可以继续以空数据工作。 */
    sealed interface LoadError {
        data class Corrupt(val archive: File?) : LoadError
        data class Failed(val cause: Throwable) : LoadError
    }

    fun startLoad() {
        scope.launch {
            fileMutex.withLock {
                when (val result = storeFile.read()) {
                    StoreFile.ReadResult.Empty -> Unit
                    is StoreFile.ReadResult.Ok -> _store.value = result.store
                    is StoreFile.ReadResult.Corrupt -> _loadError.value = LoadError.Corrupt(result.archive)
                    is StoreFile.ReadResult.Failed -> _loadError.value = LoadError.Failed(result.cause)
                }
                _loaded.value = true
            }
        }
    }

    /**
     * 应用变换并落盘，读写都在 IO 线程，返回 false 表示写入失败、内存态已回滚。
     *
     * 先更新内存态让界面立刻响应（乐观更新），失败再回滚；整个读改写过程由 [fileMutex] 串行化。
     * 加载完成前、或数据文件不可读（[LoadError.Failed]）时拒绝写入：
     * 否则会基于空的初始 [Store] 覆盖掉磁盘上的旧记录。
     */
    suspend fun update(transform: (Store) -> Store): Boolean = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            if (!_loaded.value) return@withLock false
            if (_loadError.value is LoadError.Failed) return@withLock false
            val current = _store.value
            val next = transform(current)
            if (next == current) return@withLock true
            _store.value = next
            if (storeFile.write(next)) {
                true
            } else {
                _store.value = current
                false
            }
        }
    }

    /** 供导入时整体合并使用。 */
    suspend fun replace(next: Store): Boolean = update { next }
}
