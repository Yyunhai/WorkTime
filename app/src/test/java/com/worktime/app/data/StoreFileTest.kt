package com.worktime.app.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoreFileTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val sample = Store(
        records = mapOf(
            "2026-10-19" to DayRecord(
                status = DayStatus.work,
                segments = listOf(Segment("09:00", "18:00")),
                breakMinutes = 60,
                note = "加班",
            ),
        ),
    )

    @Test
    fun missingFileReadsAsEmpty() {
        val result = StoreFile(File(folder.root, "worktime/data.json"), json).read()

        assertEquals(StoreFile.ReadResult.Empty, result)
    }

    @Test
    fun writeThenReadRoundTrips() {
        val store = StoreFile(File(folder.root, "worktime/data.json"), json)

        assertTrue(store.write(sample))
        assertEquals(StoreFile.ReadResult.Ok(sample), store.read())
    }

    @Test
    fun corruptFileIsArchivedAndCleared() {
        val dir = folder.newFolder("worktime")
        val file = File(dir, "data.json")
        file.writeText("{ this is not json")

        val corrupt = StoreFile(file, json).read() as StoreFile.ReadResult.Corrupt

        val archive = corrupt.archive
        assertNotNull(archive)
        assertTrue(archive!!.isFile)
        assertEquals("{ this is not json", archive.readText())
        assertFalse(file.exists())
    }

    @Test
    fun writeRecreatesMissingDirectories() {
        val file = File(folder.root, "nested/worktime/data.json")
        assertFalse(file.parentFile!!.exists())

        assertTrue(StoreFile(file, json).write(sample))
        assertTrue(file.isFile)
    }

    @Test
    fun overwriteExistingFileWorks() {
        val file = File(folder.root, "worktime/data.json")
        val store = StoreFile(file, json)
        val second = sample.copy(
            records = sample.records + ("2026-10-20" to DayRecord(status = DayStatus.rest)),
        )

        assertTrue(store.write(sample))
        assertTrue(store.write(second))

        assertEquals(StoreFile.ReadResult.Ok(second), store.read())
    }
}
