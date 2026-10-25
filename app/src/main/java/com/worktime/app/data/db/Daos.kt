package com.worktime.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class StatusCounts(
    val workDays: Int,
    val restDays: Int,
    val leaveDays: Int,
)

data class MonthMinutes(
    val month: String,
    val minutes: Int,
)

data class CategoryTotalRow(
    val categoryId: Long?,
    val categoryName: String,
    val totalCents: Long,
)

private const val WORK_MINUTES_SOURCE = """
    (SELECT date, SUM(endMinute - startMinute) AS minutes FROM segment GROUP BY date)
"""

@Dao
interface DayRecordDao {

    @Transaction
    @Query("SELECT * FROM day_record WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: String, to: String): Flow<List<DayRecordWithSegments>>

    @Transaction
    @Query("SELECT * FROM day_record WHERE date = :date")
    suspend fun getByDate(date: String): DayRecordWithSegments?

    @Transaction
    @Query("SELECT * FROM day_record ORDER BY date")
    suspend fun getAll(): List<DayRecordWithSegments>

    @Query("SELECT date FROM day_record")
    suspend fun allDates(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DayRecordEntity)

    @Query("DELETE FROM segment WHERE date = :date")
    suspend fun deleteSegments(date: String)

    @Insert
    suspend fun insertSegments(segments: List<SegmentEntity>)

    @Query("DELETE FROM day_record WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM day_record")
    suspend fun deleteAll()

    /** upsert 当天记录 → 删除旧时间段 → 插入新时间段，同一事务内完成。 */
    @Transaction
    suspend fun save(date: String, record: DayRecordEntity, segments: List<SegmentEntity>) {
        upsert(record)
        deleteSegments(date)
        if (segments.isNotEmpty()) insertSegments(segments)
    }

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN status = 'WORK' AND net > 0 THEN 1 ELSE 0 END), 0) AS workDays,
            COALESCE(SUM(CASE WHEN status = 'REST' THEN 1 ELSE 0 END), 0) AS restDays,
            COALESCE(SUM(CASE WHEN status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveDays
        FROM (
            SELECT d.status AS status,
                   MAX(0, COALESCE(s.minutes, 0) - d.break_minutes) AS net
            FROM day_record d
            LEFT JOIN $WORK_MINUTES_SOURCE s ON s.date = d.date
            WHERE d.date BETWEEN :from AND :to
        )
    """)
    fun observeStatusCounts(from: String, to: String): Flow<StatusCounts>

    @Query("""
        SELECT COALESCE(SUM(MAX(0, COALESCE(s.minutes, 0) - d.break_minutes)), 0)
        FROM day_record d
        LEFT JOIN $WORK_MINUTES_SOURCE s ON s.date = d.date
        WHERE d.status = 'WORK' AND d.date BETWEEN :from AND :to
    """)
    fun observeTotalMinutes(from: String, to: String): Flow<Long>

    @Query("""
        SELECT COALESCE(MAX(net), 0)
        FROM (
            SELECT MAX(0, COALESCE(s.minutes, 0) - d.break_minutes) AS net
            FROM day_record d
            LEFT JOIN $WORK_MINUTES_SOURCE s ON s.date = d.date
            WHERE d.status = 'WORK' AND d.date BETWEEN :from AND :to
        )
    """)
    fun observeMaxDailyMinutes(from: String, to: String): Flow<Long>

    @Query("""
        SELECT substr(d.date, 1, 7) AS month,
               COALESCE(SUM(MAX(0, COALESCE(s.minutes, 0) - d.break_minutes)), 0) AS minutes
        FROM day_record d
        LEFT JOIN $WORK_MINUTES_SOURCE s ON s.date = d.date
        WHERE d.status = 'WORK' AND d.date BETWEEN :from AND :to
        GROUP BY month
        ORDER BY month
    """)
    fun observeMonthlyMinutes(from: String, to: String): Flow<List<MonthMinutes>>
}

@Dao
interface ExpenseDao {

    @Query("""
        SELECT e.*, c.name AS categoryName
        FROM expense e
        LEFT JOIN expense_category c ON c.id = e.categoryId
        WHERE e.date BETWEEN :from AND :to
        ORDER BY e.date DESC, e.id DESC
    """)
    fun observeBetween(from: String, to: String): Flow<List<ExpenseWithCategory>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expense WHERE date BETWEEN :from AND :to")
    fun observeTotal(from: String, to: String): Flow<Long>

    @Query("""
        SELECT e.categoryId AS categoryId,
               COALESCE(c.name, '未分类') AS categoryName,
               COALESCE(SUM(e.amountCents), 0) AS totalCents
        FROM expense e
        LEFT JOIN expense_category c ON c.id = e.categoryId
        WHERE e.date BETWEEN :from AND :to
        GROUP BY e.categoryId
        ORDER BY totalCents DESC
    """)
    fun observeCategoryTotals(from: String, to: String): Flow<List<CategoryTotalRow>>

    @Query("SELECT date FROM expense")
    suspend fun allDates(): List<String>

    @Query("SELECT * FROM expense WHERE date BETWEEN :from AND :to ORDER BY date, id")
    suspend fun getAllBetween(from: String, to: String): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): ExpenseEntity?

    @Query("SELECT externalId FROM expense WHERE externalId IN (:ids)")
    suspend fun existingExternalIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(expense: ExpenseEntity): Long

    @Query("""
        UPDATE expense
        SET amountCents = :amountCents, categoryId = :categoryId, note = :note
        WHERE externalId = :externalId
    """)
    suspend fun updateByExternalId(
        externalId: String,
        amountCents: Long,
        categoryId: Long?,
        note: String?,
    )

    @Query("UPDATE expense SET externalId = :externalId WHERE id = :id")
    suspend fun setExternalId(id: Long, externalId: String)

    /** 兜底：任何没有 externalId 的旧行补一个唯一标识，保证界面可编辑/删除。 */
    @Query("UPDATE expense SET externalId = lower(hex(randomblob(16))) WHERE externalId IS NULL")
    suspend fun fillMissingExternalIds()

    @Query("DELETE FROM expense WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)

    @Query("DELETE FROM expense")
    suspend fun deleteAll()
}

@Dao
interface ExpenseCategoryDao {

    @Query("SELECT * FROM expense_category ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_category ORDER BY sortOrder, id")
    suspend fun getAll(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_category WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExpenseCategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM expense_category")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(category: ExpenseCategoryEntity): Long

    @Query("UPDATE expense_category SET name = :name, sortOrder = :sortOrder WHERE id = :id")
    suspend fun update(id: Long, name: String, sortOrder: Int)

    @Query("DELETE FROM expense_category WHERE id = :id")
    suspend fun delete(id: Long)
}
