package com.worktime.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 备份文件的结构，保持与旧版 JSON 一致：settings / records / expenses。
 * 字段都允许缺失，读取时用默认值兜底。
 */
@Serializable
data class BackupFile(
    val settings: BackupSettings? = null,
    val records: Map<String, BackupRecord> = emptyMap(),
    val expenses: Map<String, List<BackupExpense>> = emptyMap(),
)

@Serializable
data class BackupSettings(
    val theme: String? = null,
    val launchArt: Boolean? = null,
)

@Serializable
data class BackupRecord(
    val status: String? = null,
    val segments: List<BackupSegment>? = null,
    val breakMinutes: Int? = null,
    val note: String? = null,
)

@Serializable
data class BackupSegment(
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class BackupExpense(
    val id: String? = null,
    val amountCents: Long? = null,
    val category: String? = null,
    val note: String? = null,
)

/** 通过 SAF 读写备份文件，不需要任何存储权限。 */
object Backup {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    data class ImportPlan(val adding: Int, val overwriting: Int) {
        val affectedDays: Int get() = adding + overwriting
    }

    fun encode(data: BackupFile): String =
        json.encodeToString(BackupFile.serializer(), data)

    fun decode(text: String): BackupFile? = runCatching {
        json.decodeFromString(BackupFile.serializer(), text)
    }.getOrNull()

    suspend fun write(context: Context, data: BackupFile, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val text = encode(data)
                val stream = context.contentResolver.openOutputStream(uri, "wt")
                    ?: return@withContext false
                stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun read(context: Context, uri: Uri): BackupFile? =
        withContext(Dispatchers.IO) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().decodeToString()
                }
            } catch (_: Exception) {
                null
            } ?: return@withContext null
            decode(text)
        }

    /** 按日期统计预览：备份里出现的日期，是否已存在于本机。 */
    fun plan(currentDates: Set<String>, incoming: BackupFile): ImportPlan {
        val dates = incoming.records.keys + incoming.expenses.keys
        val overwriting = dates.count { it in currentDates }
        return ImportPlan(adding = dates.size - overwriting, overwriting = overwriting)
    }
}
