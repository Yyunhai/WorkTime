package com.worktime.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 通过 SAF 读写备份文件，不需要任何存储权限。 */
object Backup {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    data class ImportPlan(val adding: Int, val overwriting: Int) {
        val affectedDays: Int get() = adding + overwriting
    }

    suspend fun export(context: Context, store: Store, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val text = json.encodeToString(Store.serializer(), store)
                val stream = context.contentResolver.openOutputStream(uri, "wt")
                    ?: return@withContext false
                stream.use { it.write(text.toByteArray()) }
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun read(context: Context, uri: Uri): Store? =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().decodeToString()
                } ?: return@withContext null
                json.decodeFromString(Store.serializer(), text)
            } catch (_: Exception) {
                null
            }
        }

    fun plan(current: Store, incoming: Store): ImportPlan {
        val dates = incoming.records.keys + incoming.expenses.keys
        val existing = current.records.keys + current.expenses.keys
        val overwriting = dates.count { it in existing }
        return ImportPlan(adding = dates.size - overwriting, overwriting = overwriting)
    }

    /** 导入侧按日期覆盖现有的记录与支出，设置项保留本机当前值。 */
    fun merge(current: Store, incoming: Store): Store =
        current.copy(
            records = current.records + incoming.records,
            expenses = current.expenses + incoming.expenses,
        )
}
