package com.worktime.app.domain

import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Store
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** 支出的金额解析、汇总与导入清洗；和 [WorkMath] 一样不依赖 Android API。 */
object ExpenseMath {

    /** 单笔上限：1000 万元，够用且能避免金额字段溢出。 */
    private const val MAX_CENTS = 1_000_000_000L

    /** "12" / "12.3" / "12.34" → 分；空串、0、负数、超过两位小数都返回 null。 */
    fun parseAmountToCents(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split('.')
        if (parts.size > 2) return null
        val yuanText = parts[0]
        if (yuanText.isEmpty() || yuanText.any { !it.isDigit() }) return null
        val yuan = yuanText.toLongOrNull() ?: return null
        if (yuan > MAX_CENTS / 100) return null
        val fraction = parts.getOrNull(1) ?: ""
        if (fraction.length > 2 || fraction.any { !it.isDigit() }) return null
        val cents = fraction.padEnd(2, '0').toLongOrNull() ?: 0L
        val total = yuan * 100 + cents
        return if (total in 1..MAX_CENTS) total else null
    }

    /** 不带千分位，用于输入框回填。 */
    fun formatPlain(cents: Long): String =
        "%d.%02d".format(Locale.US, cents / 100, (cents % 100).toInt())

    /** 带千分位，用于展示。 */
    fun formatCents(cents: Long): String =
        "%,.2f".format(Locale.US, cents / 100.0)

    /** 该月所有支出，按日期升序。 */
    fun entriesIn(month: YearMonth, store: Store): List<Pair<LocalDate, Expense>> =
        (1..month.lengthOfMonth()).flatMap { day ->
            val date = month.atDay(day)
            store.expenses[date.toString()].orEmpty().map { date to it }
        }

    fun monthTotalCents(month: YearMonth, store: Store): Long =
        entriesIn(month, store).sumOf { it.second.amountCents }

    fun dayTotalCents(date: LocalDate, store: Store): Long =
        store.expenses[date.toString()].orEmpty().sumOf { it.amountCents }

    /** 按金额从大到小排序的分类汇总。 */
    fun byCategory(month: YearMonth, store: Store): List<Pair<ExpenseCategory, Long>> =
        entriesIn(month, store)
            .groupBy({ it.second.category }, { it.second.amountCents })
            .map { (category, amounts) -> category to amounts.sum() }
            .sortedByDescending { it.second }

    /** 过滤掉金额非法、备注过长或 id 重复的支出；导入备份时用。 */
    fun sanitize(store: Store): Store {
        val cleaned = store.expenses.mapValues { (_, list) ->
            list.filter { it.amountCents in 1..MAX_CENTS && it.note.length <= 200 }
                .distinctBy { it.id }
        }.filterValues { it.isNotEmpty() }
        return store.copy(expenses = cleaned)
    }
}
