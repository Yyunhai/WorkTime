package com.worktime.app.data

import android.content.Context
import androidx.room.withTransaction
import com.worktime.app.data.db.AppDatabase
import com.worktime.app.data.db.DayRecordEntity
import com.worktime.app.data.db.ExpenseCategoryEntity
import com.worktime.app.data.db.ExpenseEntity
import com.worktime.app.data.db.SegmentEntity
import com.worktime.app.domain.WorkMath
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 一次性把旧版 data.json 迁移进 Room。
 *
 * - 文件不存在直接跳过；
 * - 解析和写入在同一个事务里，失败整体回滚，旧文件保持不动，下次启动重试；
 * - 成功后把旧文件改名为 data.json.bak，并把旧 theme 写进 DataStore。
 */
class LegacyJsonImporter(
    context: Context,
    private val db: AppDatabase,
) {

    private val context = context.applicationContext
    private val settingsStore = SettingsStore(this.context)
    private val legacyFile = File(File(this.context.filesDir, "worktime"), "data.json")

    suspend fun migrateIfNeeded(): Boolean {
        if (!legacyFile.isFile) return false
        val text = withContext(Dispatchers.IO) { legacyFile.readText() }
        val parsed = parse(text)

        db.withTransaction {
            for (record in parsed.records) {
                db.dayRecordDao().save(record.date, record.entity, record.segments)
            }
            for (expense in parsed.expenses) {
                val categoryId = ensureCategory(CategoryMapping.nameForLegacy(expense.category))
                db.expenseDao().insertIgnore(
                    ExpenseEntity(
                        date = expense.date,
                        amountCents = expense.amountCents,
                        categoryId = categoryId,
                        note = expense.note,
                        externalId = expense.externalId,
                    ),
                )
            }
        }

        parsed.theme?.let { settingsStore.setThemeName(it) }
        archive()
        return true
    }

    private suspend fun ensureCategory(name: String?): Long? {
        if (name.isNullOrBlank()) return null
        val dao = db.expenseCategoryDao()
        dao.findByName(name)?.let { return it.id }
        val sortOrder = dao.nextSortOrder()
        val id = dao.insertIgnore(ExpenseCategoryEntity(name = name, sortOrder = sortOrder))
        return if (id != -1L) id else dao.findByName(name)?.id
    }

    private fun archive(): Boolean {
        val target = File(legacyFile.parentFile, "data.json.bak")
        if (target.exists()) target.delete()
        if (legacyFile.renameTo(target)) return true
        return runCatching {
            legacyFile.copyTo(target, overwrite = true)
            legacyFile.delete()
            true
        }.getOrDefault(false)
    }

    private fun parse(text: String): ParsedData {
        val root = JSONObject(text)
        val theme = root.optJSONObject("settings")
            ?.optString("theme")
            ?.takeIf { it.isNotBlank() }

        val records = mutableListOf<ParsedRecord>()
        root.optJSONObject("records")?.let { recordsObj ->
            for (date in recordsObj.keys()) {
                if (!isValidDate(date)) continue
                val obj = recordsObj.optJSONObject(date) ?: continue
                val status = parseStatus(obj.optString("status"))
                val segments = mutableListOf<SegmentEntity>()
                obj.optJSONArray("segments")?.let { array ->
                    for (index in 0 until array.length()) {
                        val segment = array.optJSONObject(index) ?: continue
                        val start = WorkMath.parseClock(segment.optString("start")) ?: continue
                        val end = WorkMath.parseClock(segment.optString("end")) ?: continue
                        segments += SegmentEntity(
                            date = date,
                            startMinute = start,
                            endMinute = if (end <= start) end + 1440 else end,
                        )
                    }
                }
                records += ParsedRecord(
                    date = date,
                    entity = DayRecordEntity(
                        date = date,
                        status = status.name.uppercase(),
                        leaveType = null,
                        note = obj.optString("note").takeIf { it.isNotBlank() },
                        breakMinutes = obj.optInt("breakMinutes", 0).coerceIn(0, 720),
                    ),
                    segments = segments,
                )
            }
        }

        val expenses = mutableListOf<ParsedExpense>()
        root.optJSONObject("expenses")?.let { expensesObj ->
            for (date in expensesObj.keys()) {
                if (!isValidDate(date)) continue
                val array = expensesObj.optJSONArray(date) ?: continue
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val amount = item.optLong("amountCents", -1L)
                    if (amount <= 0L) continue
                    val rawId = item.optString("id").takeIf { it.isNotBlank() }
                    // 旧数据里没有 id 时用确定性 UUID：即使迁移重跑也不会重复插入。
                    val externalId = rawId ?: UUID.nameUUIDFromBytes(
                        "legacy:$date:$index:$amount".toByteArray(Charsets.UTF_8),
                    ).toString()
                    expenses += ParsedExpense(
                        date = date,
                        amountCents = amount,
                        category = item.optString("category").takeIf { it.isNotBlank() },
                        note = item.optString("note").takeIf { it.isNotBlank() },
                        externalId = externalId,
                    )
                }
            }
        }

        return ParsedData(theme, records, expenses)
    }

    private fun parseStatus(raw: String): com.worktime.app.data.DayStatus =
        when (raw.lowercase()) {
            "rest" -> com.worktime.app.data.DayStatus.rest
            "leave" -> com.worktime.app.data.DayStatus.leave
            else -> com.worktime.app.data.DayStatus.work
        }

    private fun isValidDate(key: String): Boolean =
        runCatching { java.time.LocalDate.parse(key) }.isSuccess

    private data class ParsedData(
        val theme: String?,
        val records: List<ParsedRecord>,
        val expenses: List<ParsedExpense>,
    )

    private data class ParsedRecord(
        val date: String,
        val entity: DayRecordEntity,
        val segments: List<SegmentEntity>,
    )

    private data class ParsedExpense(
        val date: String,
        val amountCents: Long,
        val category: String?,
        val note: String?,
        val externalId: String?,
    )
}
