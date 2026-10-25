package com.worktime.app.data

import java.time.LocalDate

enum class DayStatus { work, rest, leave }

data class Segment(
    val start: String,
    val end: String,
    /** 结束时间落在次日（跨午夜）；end 仍是 00:00–23:59 的时钟时间。 */
    val endNextDay: Boolean = false,
)

data class DayRecord(
    val status: DayStatus,
    val segments: List<Segment> = emptyList(),
    val breakMinutes: Int = 0,
    val note: String = "",
)

enum class ThemeMode { system, light, dark, pink }

data class Settings(
    val theme: ThemeMode = ThemeMode.system,
    val launchArt: Boolean = true,
)

/** 支出分类，来自 expense_category 表。 */
data class ExpenseCategory(
    val id: Long,
    val name: String,
    val sortOrder: Int,
)

/**
 * 单笔支出。金额以「分」为单位存整数。
 * [id] 对应数据库的 externalId（旧 JSON 里的 UUID，或新建时生成的 UUID），界面用它编辑/删除。
 */
data class Expense(
    val id: String,
    val date: LocalDate,
    val amountCents: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val note: String,
)
