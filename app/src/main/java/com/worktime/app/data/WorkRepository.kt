package com.worktime.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.worktime.app.data.db.AppDatabase
import com.worktime.app.data.db.CategoryTotalRow
import com.worktime.app.data.db.DayRecordEntity
import com.worktime.app.data.db.DayRecordWithSegments
import com.worktime.app.data.db.ExpenseCategoryEntity
import com.worktime.app.data.db.ExpenseEntity
import com.worktime.app.data.db.ExpenseWithCategory
import com.worktime.app.data.db.MonthMinutes
import com.worktime.app.data.db.SegmentEntity
import com.worktime.app.data.db.StatusCounts
import com.worktime.app.domain.CategoryTotal
import com.worktime.app.domain.MonthData
import com.worktime.app.domain.WorkMath
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 数据访问入口：Room 负责记录与支出，DataStore 负责设置。 */
class WorkRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val settingsStore = SettingsStore(appContext)
    private val importer = LegacyJsonImporter(appContext, db)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    val settings: StateFlow<Settings> = settingsStore.settings
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    val categories: Flow<List<ExpenseCategory>> = db.expenseCategoryDao().observeAll()
        .map { list -> list.map { ExpenseCategory(it.id, it.name, it.sortOrder) } }

    /** 启动时执行一次性迁移，完成后才允许界面编辑。 */
    fun start() {
        scope.launch {
            runCatching {
                importer.migrateIfNeeded()
                // 旧 JSON 里没有 id 的支出也要有界面标识；迁移后统一补齐。
                db.expenseDao().fillMissingExternalIds()
                // 等设置真正读出来再放行界面，避免开屏图/主题先用默认值。
                settingsStore.settings.first()
            }.onFailure { error ->
                _loadError.value = "旧数据迁移失败，下次启动会重试：${error.message ?: error}"
            }
            _loaded.value = true
        }
    }

    fun observeMonth(month: YearMonth): Flow<MonthData> {
        val from = month.atDay(1).toString()
        val to = month.atEndOfMonth().toString()
        val trendFrom = month.minusMonths(5).atDay(1).toString()
        val dayDao = db.dayRecordDao()
        val expenseDao = db.expenseDao()

        val recordsPart = combine(
            dayDao.observeBetween(from, to),
            dayDao.observeStatusCounts(from, to),
            dayDao.observeTotalMinutes(from, to),
            dayDao.observeMaxDailyMinutes(from, to),
        ) { rows, counts, total, max ->
            RecordsPart(rows, counts, total, max)
        }

        val expensesPart = combine(
            dayDao.observeMonthlyMinutes(trendFrom, to),
            expenseDao.observeBetween(from, to),
            expenseDao.observeTotal(from, to),
            expenseDao.observeCategoryTotals(from, to),
        ) { trend, rows, total, categories ->
            ExpensesPart(trend, rows, total, categories)
        }

        return combine(recordsPart, expensesPart) { records, expenses ->
            buildMonthData(month, records, expenses)
        }
    }

    suspend fun saveDay(date: LocalDate, record: DayRecord): Boolean = runCatching {
        val key = date.toString()
        val segments = record.segments.mapNotNull { segment ->
            val start = WorkMath.parseClock(segment.start) ?: return@mapNotNull null
            val end = WorkMath.parseClock(segment.end) ?: return@mapNotNull null
            SegmentEntity(
                date = key,
                startMinute = start,
                endMinute = if (end <= start) end + 1440 else end,
            )
        }
        db.dayRecordDao().save(
            date = key,
            record = DayRecordEntity(
                date = key,
                status = record.status.name.uppercase(),
                leaveType = null,
                note = record.note.ifBlank { null },
                breakMinutes = record.breakMinutes,
            ),
            segments = segments,
        )
        true
    }.getOrDefault(false)

    suspend fun deleteDay(date: LocalDate): Boolean = runCatching {
        db.dayRecordDao().delete(date.toString())
        true
    }.getOrDefault(false)

    suspend fun addExpense(
        date: LocalDate,
        amountCents: Long,
        categoryId: Long?,
        note: String,
    ): Boolean = runCatching {
        db.expenseDao().insertIgnore(
            ExpenseEntity(
                date = date.toString(),
                amountCents = amountCents,
                categoryId = categoryId,
                note = note.ifBlank { null },
                externalId = UUID.randomUUID().toString(),
            ),
        )
        true
    }.getOrDefault(false)

    suspend fun updateExpense(expense: Expense): Boolean = runCatching {
        if (expense.id.isBlank()) return@runCatching false
        db.expenseDao().updateByExternalId(
            externalId = expense.id,
            amountCents = expense.amountCents,
            categoryId = expense.categoryId,
            note = expense.note.ifBlank { null },
        )
        true
    }.getOrDefault(false)

    suspend fun deleteExpense(id: String): Boolean = runCatching {
        if (id.isBlank()) return@runCatching false
        db.expenseDao().deleteByExternalId(id)
        true
    }.getOrDefault(false)

    suspend fun addCategory(name: String): Boolean = runCatching {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@runCatching false
        val dao = db.expenseCategoryDao()
        dao.insertIgnore(ExpenseCategoryEntity(name = trimmed, sortOrder = dao.nextSortOrder()))
        true
    }.getOrDefault(false)

    suspend fun renameCategory(id: Long, name: String): Boolean = runCatching {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@runCatching false
        val dao = db.expenseCategoryDao()
        val current = dao.getAll().firstOrNull { it.id == id } ?: return@runCatching false
        dao.update(id, trimmed, current.sortOrder)
        true
    }.getOrDefault(false)

    suspend fun deleteCategory(id: Long): Boolean = runCatching {
        db.expenseCategoryDao().delete(id)
        true
    }.getOrDefault(false)

    suspend fun setTheme(mode: ThemeMode) = settingsStore.setTheme(mode)

    suspend fun setLaunchArt(enabled: Boolean) = settingsStore.setLaunchArt(enabled)

    suspend fun clearAll(): Boolean = runCatching {
        db.withTransaction {
            db.expenseDao().deleteAll()
            db.dayRecordDao().deleteAll()
        }
        true
    }.getOrDefault(false)

    suspend fun exportTo(uri: Uri): Boolean {
        val data = buildBackupFile()
        return Backup.write(appContext, data, uri)
    }

    suspend fun previewImport(uri: Uri): ImportPreview? {
        val file = Backup.read(appContext, uri) ?: return null
        val currentDates = db.dayRecordDao().allDates().toSet() + db.expenseDao().allDates().toSet()
        return ImportPreview(Backup.plan(currentDates, file), file)
    }

    suspend fun applyImport(file: BackupFile): Boolean = runCatching {
        db.withTransaction {
            for ((date, record) in file.records) {
                if (!isValidDate(date)) continue
                val entity = DayRecordEntity(
                    date = date,
                    status = parseStatus(record.status).name.uppercase(),
                    leaveType = null,
                    note = record.note?.ifBlank { null },
                    breakMinutes = (record.breakMinutes ?: 0).coerceIn(0, 720),
                )
                val segments = record.segments.orEmpty().mapNotNull { segment ->
                    val start = WorkMath.parseClock(segment.start.orEmpty()) ?: return@mapNotNull null
                    val end = WorkMath.parseClock(segment.end.orEmpty()) ?: return@mapNotNull null
                    SegmentEntity(
                        date = date,
                        startMinute = start,
                        endMinute = if (end <= start) end + 1440 else end,
                    )
                }
                db.dayRecordDao().save(date, entity, segments)
            }

            for ((date, expenses) in file.expenses) {
                if (!isValidDate(date)) continue
                for (item in expenses) {
                    val amount = item.amountCents ?: continue
                    if (amount <= 0L) continue
                    val externalId = item.id?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString()
                    if (db.expenseDao().findByExternalId(externalId) != null) continue
                    db.expenseDao().insertIgnore(
                        ExpenseEntity(
                            date = date,
                            amountCents = amount,
                            categoryId = ensureCategory(CategoryMapping.nameForLegacy(item.category)),
                            note = item.note?.ifBlank { null },
                            externalId = externalId,
                        ),
                    )
                }
            }
        }
        true
    }.getOrDefault(false)

    private suspend fun buildBackupFile(): BackupFile {
        val settings = settingsStore.settings.first()
        val categoriesById = db.expenseCategoryDao().getAll().associateBy { it.id }

        val records = mutableMapOf<String, BackupRecord>()
        for (row in db.dayRecordDao().getAll()) {
            records[row.record.date] = BackupRecord(
                status = row.record.status.lowercase(),
                segments = row.segments.sortedBy { it.id }.map {
                    BackupSegment(
                        start = WorkMath.formatMinuteOfDay(it.startMinute),
                        end = WorkMath.formatMinuteOfDay(it.endMinute),
                    )
                },
                breakMinutes = row.record.breakMinutes,
                note = row.record.note,
            )
        }

        val expenses = mutableMapOf<String, MutableList<BackupExpense>>()
        for (row in db.expenseDao().getAllBetween("0000-01-01", "9999-12-31")) {
            var externalId = row.externalId
            if (externalId.isNullOrBlank()) {
                externalId = UUID.randomUUID().toString()
                db.expenseDao().setExternalId(row.id, externalId)
            }
            val categoryName = row.categoryId?.let { categoriesById[it]?.name }
            expenses.getOrPut(row.date) { mutableListOf() } += BackupExpense(
                id = externalId,
                amountCents = row.amountCents,
                category = CategoryMapping.legacyForName(categoryName),
                note = row.note,
            )
        }

        return BackupFile(
            settings = BackupSettings(theme = settings.theme.name, launchArt = settings.launchArt),
            records = records,
            expenses = expenses,
        )
    }

    private suspend fun ensureCategory(name: String?): Long? {
        if (name.isNullOrBlank()) return null
        val dao = db.expenseCategoryDao()
        dao.findByName(name)?.let { return it.id }
        val sortOrder = dao.nextSortOrder()
        val id = dao.insertIgnore(ExpenseCategoryEntity(name = name, sortOrder = sortOrder))
        return if (id != -1L) id else dao.findByName(name)?.id
    }

    private fun buildMonthData(month: YearMonth, records: RecordsPart, expenses: ExpensesPart): MonthData {
        val details = records.rows.map { it.toDomainPair() }
        val byDate = details.associate { it.first.toString() to it.second }
        val byMonth = expenses.trend.associate { it.month to it.minutes }

        return MonthData(
            records = byDate,
            details = details,
            summary = WorkMath.MonthSummary(
                workDays = records.counts.workDays,
                restDays = records.counts.restDays,
                leaveDays = records.counts.leaveDays,
                totalMinutes = records.total.toInt(),
            ),
            trend = WorkMath.lastMonths(month, 6).map { it to (byMonth[it.toString()] ?: 0) },
            maxDailyMinutes = records.max.toInt(),
            expenses = expenses.rows.map { it.toDomain() },
            expenseTotalCents = expenses.total,
            categoryTotals = expenses.categories.map {
                CategoryTotal(it.categoryId, it.categoryName, it.totalCents)
            },
        )
    }

    private fun DayRecordWithSegments.toDomainPair(): Pair<LocalDate, DayRecord> {
        val date = LocalDate.parse(record.date)
        return date to DayRecord(
            status = statusOf(record.status),
            segments = segments.sortedBy { it.id }.map { segment ->
                Segment(
                    start = WorkMath.formatMinuteOfDay(segment.startMinute),
                    end = WorkMath.formatMinuteOfDay(segment.endMinute),
                )
            },
            breakMinutes = record.breakMinutes,
            note = record.note.orEmpty(),
        )
    }

    private fun ExpenseWithCategory.toDomain(): Expense = Expense(
        id = expense.externalId.orEmpty(),
        date = LocalDate.parse(expense.date),
        amountCents = expense.amountCents,
        categoryId = expense.categoryId,
        categoryName = categoryName,
        note = expense.note.orEmpty(),
    )

    private fun statusOf(raw: String): DayStatus = when (raw.uppercase()) {
        "REST" -> DayStatus.rest
        "LEAVE" -> DayStatus.leave
        else -> DayStatus.work
    }

    private fun parseStatus(raw: String?): DayStatus = when (raw?.lowercase()) {
        "rest" -> DayStatus.rest
        "leave" -> DayStatus.leave
        else -> DayStatus.work
    }

    private fun isValidDate(key: String): Boolean =
        runCatching { LocalDate.parse(key) }.isSuccess

    data class ImportPreview(val plan: Backup.ImportPlan, val file: BackupFile)

    private data class RecordsPart(
        val rows: List<DayRecordWithSegments>,
        val counts: StatusCounts,
        val total: Long,
        val max: Long,
    )

    private data class ExpensesPart(
        val trend: List<MonthMinutes>,
        val rows: List<ExpenseWithCategory>,
        val total: Long,
        val categories: List<CategoryTotalRow>,
    )
}
