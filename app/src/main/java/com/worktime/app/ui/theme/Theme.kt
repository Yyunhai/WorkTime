package com.worktime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.worktime.app.data.ThemeMode

@Composable
fun WorkTimeTheme(
    mode: ThemeMode = ThemeMode.system,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.system -> isSystemInDarkTheme()
        ThemeMode.light -> false
        ThemeMode.dark -> true
    }

    CompositionLocalProvider(
        LocalDayInk provides if (dark) DarkDayInk else LightDayInk,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
