package com.worktime.app.domain

import com.worktime.app.data.Expense
import com.worktime.app.data.ExpenseCategory
import com.worktime.app.data.Store
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseMathTest {

    @Test
    fun parseAmounts() {
        assertEquals(1200L, ExpenseMath.parseAmountToCents("12"))
        assertEquals(1230L, ExpenseMath.parseAmountToCents("12.3"))
        assertEquals(1234L, ExpenseMath.parseAmountToCents("12.34"))
        assertEquals(1L, ExpenseMath.parseAmountToCents("0.01"))
        assertEquals(850L, ExpenseMath.parseAmountToCents(" 8.50 "))
    }

    @Test
    fun rejectInvalidAmounts() {
        listOf("", " ", "0", "0.00", "-1", "1.234", "abc", "1..2", ".5", "10,00")
            .forEach { assertNull(it, ExpenseMath.parseAmountToCents(it)) }
    }

    @Test
    fun rejectAmountsOverTheLimit() {
        assertNull(ExpenseMath.parseAmountToCents("10000000.01"))
    }

    @Test
    fun formatAmounts() {
        assertEquals("12.34", ExpenseMath.formatCents(1234))
        assertEquals("0.05", ExpenseMath.formatCents(5))
        assertEquals("1,000.00", ExpenseMath.formatCents(100_000))
        assertEquals("12.34", ExpenseMath.formatPlain(1234))
        assertEquals("0.05", ExpenseMath.formatPlain(5))
    }

    @Test
    fun monthTotalsAndCategoryBreakdown() {
        val month = YearMonth.of(2026, 10)
        val store = Store(
            expenses = mapOf(
                "2026-10-08" to listOf(
                    expense("a", 1200, ExpenseCategory.food),
                    expense("b", 300, ExpenseCategory.transport),
                ),
                "2026-10-09" to listOf(expense("c", 2500, ExpenseCategory.food)),
                "2026-09-30" to listOf(expense("d", 9999, ExpenseCategory.shopping)),
            ),
        )

        assertEquals(4000L, ExpenseMath.monthTotalCents(month, store))
        assertEquals(
            listOf(ExpenseCategory.food to 3700L, ExpenseCategory.transport to 300L),
            ExpenseMath.byCategory(month, store),
        )
        assertEquals(
            1500L,
            ExpenseMath.dayTotalCents(LocalDate.of(2026, 10, 8), store),
        )
    }

    @Test
    fun sanitizeDropsInvalidEntries() {
        val store = Store(
            expenses = mapOf(
                "2026-10-08" to listOf(
                    expense("ok", 500, ExpenseCategory.food),
                    expense("zero", 0, ExpenseCategory.other),
                    expense("negative", -1, ExpenseCategory.other),
                    expense("ok", 700, ExpenseCategory.food),
                ),
                "2026-10-09" to listOf(expense("huge", 2_000_000_000, ExpenseCategory.other)),
            ),
        )

        val clean = ExpenseMath.sanitize(store)

        assertEquals(setOf("2026-10-08"), clean.expenses.keys)
        assertEquals(listOf("ok"), clean.expenses.getValue("2026-10-08").map { it.id })
        assertEquals(500L, clean.expenses.getValue("2026-10-08").single().amountCents)
    }

    private fun expense(id: String, cents: Long, category: ExpenseCategory) =
        Expense(id = id, amountCents = cents, category = category)
}
