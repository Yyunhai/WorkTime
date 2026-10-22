package com.worktime.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "day_record")
data class DayRecordEntity(
    @PrimaryKey val date: String,
    val status: String,
    val leaveType: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "break_minutes") val breakMinutes: Int = 0,
)

@Entity(
    tableName = "segment",
    foreignKeys = [
        ForeignKey(
            entity = DayRecordEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date")],
)
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startMinute: Int,
    val endMinute: Int,
)

@Entity(
    tableName = "expense_category",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("date"), Index("categoryId"), Index(value = ["externalId"], unique = true)],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amountCents: Long,
    val categoryId: Long? = null,
    val note: String? = null,
    val externalId: String? = null,
)

/** 一天 + 当天所有时间段。 */
data class DayRecordWithSegments(
    @Embedded val record: DayRecordEntity,
    @Relation(parentColumn = "date", entityColumn = "date")
    val segments: List<SegmentEntity>,
)

/** 一笔支出 + 分类名；分类被删除或未分类时 categoryName 为 null。 */
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    val categoryName: String?,
)
