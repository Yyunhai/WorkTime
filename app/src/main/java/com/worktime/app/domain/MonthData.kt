package com.worktime.app.domain

import com.worktime.app.data.DayRecord
import com.worktime.app.data.Expense
import java.time.LocalDate
import java.time.YearMonth

/** 支出分类汇总行，[categoryId] 为 null 表示「未分类」。 */
data class CategoryTotal(
    val categoryId: Long?,
    val name: String,
    val amountCents: Long,
)

/** 当前选中月份需要的全部数据，由数据库按日期范围查询后组装。 */
data class MonthData(
    val records: Map<String, DayRecord> = emptyMap(),
    val details: List<Pair<LocalDate, DayRecord>> = emptyList(),
    val summary: WorkMath.MonthSummary = WorkMath.MonthSummary(0, 0, 0, 0),
    val trend: List<Pair<YearMonth, Int>> = emptyList(),
    val maxDailyMinutes: Int = 0,
    val expenses: List<Expense> = emptyList(),
    val expenseTotalCents: Long = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
) {
    companion object {
        val Empty = MonthData()
    }
}
