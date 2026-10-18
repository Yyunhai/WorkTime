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

@Serializable
data class Store(
    val version: Int = 1,
    val settings: Settings = Settings(),
    val records: Map<String, DayRecord> = emptyMap(),
)
