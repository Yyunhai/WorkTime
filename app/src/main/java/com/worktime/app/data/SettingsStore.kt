package com.worktime.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** 设置项用 DataStore 保存，不进 Room。 */
class SettingsStore(context: Context) {

    private val context = context.applicationContext

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val launchArt = booleanPreferencesKey("launch_art")
    }

    val settings: Flow<Settings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            Settings(
                theme = themeOf(prefs[Keys.theme]),
                launchArt = prefs[Keys.launchArt] ?: true,
            )
        }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setLaunchArt(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.launchArt] = enabled }
    }

    /** 旧 JSON 迁移专用：把字符串主题写进 DataStore。 */
    suspend fun setThemeName(name: String?) {
        context.settingsDataStore.edit { it[Keys.theme] = themeOf(name).name }
    }

    private fun themeOf(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ThemeMode.system
}
