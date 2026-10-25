package com.worktime.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupJsonTest {

    @Test
    fun decodeSampleWithMissingFields() {
        val text = """
            {
              "records": {
                "2026-10-09": {
                  "status": "work",
                  "segments": [ { "start": "11:00", "end": "18:30" } ]
                }
              },
              "expenses": {
                "2026-10-19": [
                  { "id": "abc", "amountCents": 200 },
                  { "amountCents": 300, "category": "shopping", "note": "可乐" }
                ]
              }
            }
        """.trimIndent()

        val file = Backup.decode(text)
        assertNotNull(file)
        file!!

        assertEquals("work", file.records["2026-10-09"]?.status)
        assertEquals(1, file.records["2026-10-09"]?.segments?.size)
        assertEquals("11:00", file.records["2026-10-09"]?.segments?.first()?.start)
        assertEquals(2, file.expenses["2026-10-19"]?.size)
        assertNull(file.expenses["2026-10-19"]?.get(0)?.category)
        assertNull(file.expenses["2026-10-19"]?.get(1)?.id)
        assertEquals(300L, file.expenses["2026-10-19"]?.get(1)?.amountCents)
    }

    @Test
    fun roundTripKeepsSettingsAndExpenses() {
        val original = BackupFile(
            settings = BackupSettings(theme = "dark", launchArt = false),
            records = mapOf(
                "2026-10-09" to BackupRecord(
                    status = "work",
                    segments = listOf(BackupSegment("11:00", "18:30")),
                    breakMinutes = 60,
                    note = "加班",
                ),
            ),
            expenses = mapOf(
                "2026-10-19" to listOf(
                    BackupExpense(id = "e1", amountCents = 200, category = "food", note = "午饭"),
                ),
            ),
        )

        val decoded = Backup.decode(Backup.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun planCountsNewAndOverwrittenDays() {
        val incoming = BackupFile(
            records = mapOf(
                "2026-10-09" to BackupRecord(status = "work"),
                "2026-10-10" to BackupRecord(status = "rest"),
            ),
            expenses = mapOf(
                "2026-10-10" to listOf(BackupExpense(amountCents = 100)),
            ),
        )

        val plan = Backup.plan(setOf("2026-10-09"), incoming)

        assertEquals(1, plan.adding)
        assertEquals(1, plan.overwriting)
        assertEquals(2, plan.affectedDays)
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(Backup.decode("{"))
    }
}
