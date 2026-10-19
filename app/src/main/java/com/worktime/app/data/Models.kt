package com.worktime.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class DayStatus { work, rest, leave }

@Serializable
data class Segment(val start: String, val end: String)

@Serializable
data class DayRecord(
    val status: DayStatus,
    val segments: List<Segment> = emptyList(),
    val breakMinutes: Int = 0,
    val note: String = "",
)

@Serializable
enum class ThemeMode { system, light, dark }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.system,
    val launchArt: Boolean = true,
)

/** 支出分类；顺序即界面上药丸的排列顺序。 */
@Serializable
enum class ExpenseCategory { food, transport, shopping, home, entertainment, health, other }

/** 单笔支出。金额以「分」为单位存整数，避免浮点误差。 */
@Serializable
data class Expense(
    val id: String,
    val amountCents: Long,
    val category: ExpenseCategory = ExpenseCategory.other,
    val note: String = "",
)

@Serializable
data class Store(
    val version: Int = 2,
    val settings: Settings = Settings(),
    val records: Map<String, DayRecord> = emptyMap(),
    val expenses: Map<String, List<Expense>> = emptyMap(),
)
