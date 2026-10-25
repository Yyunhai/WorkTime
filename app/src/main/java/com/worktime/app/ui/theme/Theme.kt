package com.worktime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.worktime.app.data.ThemeMode

/** 昔涟主题下卡片/格子的半透明度；其它主题保持不透明。 */
val LocalCardAlpha = staticCompositionLocalOf { 1f }

/** 顶栏用的透明度，比卡片更实一点，保证标题可读。 */
val LocalSurfaceAlpha = staticCompositionLocalOf { 1f }

/** 当前是否是昔涟主题，供按钮/金额做专属配色。 */
val LocalPinkTheme = staticCompositionLocalOf { false }

@Composable
fun WorkTimeTheme(
    mode: ThemeMode = ThemeMode.system,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.system -> isSystemInDarkTheme()
        ThemeMode.light, ThemeMode.pink -> false
        ThemeMode.dark -> true
    }

    val scheme = when {
        mode == ThemeMode.pink -> PinkScheme
        dark -> DarkScheme
        else -> LightScheme
    }

    val dayInk = when {
        mode == ThemeMode.pink -> PinkDayInk
        dark -> DarkDayInk
        else -> LightDayInk
    }

    CompositionLocalProvider(
        LocalDayInk provides dayInk,
        LocalExpenseInk provides if (dark) DarkExpenseInk else LightExpenseInk,
        // 卡片/格子半透明，让背景图透出来；顶栏稍微实一点。
        LocalCardAlpha provides if (mode == ThemeMode.pink) 0.76f else 1f,
        LocalSurfaceAlpha provides if (mode == ThemeMode.pink) 0.92f else 1f,
        LocalPinkTheme provides (mode == ThemeMode.pink),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}
