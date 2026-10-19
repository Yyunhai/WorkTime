package com.worktime.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupTest {

    private val expense = Expense(id = "e1", amountCents = 1200, category = ExpenseCategory.food)

    @Test
    fun planCountsRecordAndExpenseDays() {
        val current = Store(
            records = mapOf("2026-10-01" to DayRecord(status = DayStatus.work)),
        )
        val incoming = Store(
            records = mapOf("2026-10-01" to DayRecord(status = DayStatus.rest)),
            expenses = mapOf("2026-10-02" to listOf(expense)),
        )

        val plan = Backup.plan(current, incoming)

        assertEquals(1, plan.adding)
        assertEquals(1, plan.overwriting)
        assertEquals(2, plan.affectedDays)
    }

    @Test
    fun mergeOverridesDaysAndKeepsLocalSettings() {
        val current = Store(
            settings = Settings(theme = ThemeMode.dark),
            records = mapOf("2026-10-01" to DayRecord(status = DayStatus.rest)),
        )
        val incoming = Store(
            settings = Settings(theme = ThemeMode.light),
            records = mapOf("2026-10-02" to DayRecord(status = DayStatus.work)),
            expenses = mapOf("2026-10-02" to listOf(expense)),
        )

        val merged = Backup.merge(current, incoming)

        assertEquals(ThemeMode.dark, merged.settings.theme)
        assertEquals(2, merged.records.size)
        assertEquals(listOf(expense), merged.expenses["2026-10-02"])
    }
}
