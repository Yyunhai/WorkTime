package com.worktime.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.worktime.app.data.DayStatus
import com.worktime.app.data.ThemeMode

/**
 * 手写调色板（紫 + 玫瑰 + 中性偏冷灰），刻意不用动态取色：
 * Monet 会跟着壁纸变，浅色下经常出现脏灰底 + 低对比文字，不像设计过的界面。
 */
val LightScheme = lightColorScheme(
    primary = Color(0xFF6A4C93),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DCFF),
    onPrimaryContainer = Color(0xFF23134A),

    secondary = Color(0xFF7B5E77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DCEF),
    onSecondaryContainer = Color(0xFF2F172E),

    tertiary = Color(0xFFB0447C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E7),
    onTertiaryContainer = Color(0xFF3B0A2B),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFFCF8FD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFCF8FD),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E0EC),
    onSurfaceVariant = Color(0xFF4A4453),
    outline = Color(0xFF7C7585),
    outlineVariant = Color(0xFFCDC4D0),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF1EBF4),
    surfaceContainerHigh = Color(0xFFEBE5EE),
    surfaceContainerHighest = Color(0xFFE5DFE9),

    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFC9AEFF),
    onPrimary = Color(0xFF3A1F63),
    primaryContainer = Color(0xFF513A78),
    onPrimaryContainer = Color(0xFFE9DCFF),

    secondary = Color(0xFFE0B7D6),
    onSecondary = Color(0xFF47294A),
    secondaryContainer = Color(0xFF603F5D),
    onSecondaryContainer = Color(0xFFF7DCEF),

    tertiary = Color(0xFFFFA8CF),
    onTertiary = Color(0xFF5A1638),
    tertiaryContainer = Color(0xFF772C52),
    onTertiaryContainer = Color(0xFFFFD9E7),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF36323B),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF48454E),

    surfaceContainerLowest = Color(0xFF0E0C12),
    surfaceContainerLow = Color(0xFF1C1921),
    surfaceContainer = Color(0xFF242129),
    surfaceContainerHigh = Color(0xFF2C2A33),
    surfaceContainerHighest = Color(0xFF35323B),

    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
)

/**
 * 昔涟主题：纯白 + 樱粉/白粉打底，浅紫罗兰只做少量点缀。
 * 固定浅色，不跟随系统深色。
 */
val PinkScheme = lightColorScheme(
    primary = Color(0xFFB94E78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4EE),
    onPrimaryContainer = Color(0xFF5A1F36),

    secondary = Color(0xFFE58AAE),
    onSecondary = Color(0xFF5A2338),
    secondaryContainer = Color(0xFFFFF0F5),
    onSecondaryContainer = Color(0xFF5A2338),

    tertiary = Color(0xFFC3A6E3),
    onTertiary = Color(0xFF3A2A4D),
    tertiaryContainer = Color(0xFFF6F0FC),
    onTertiaryContainer = Color(0xFF3A2A4D),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFFFF9FB),
    onBackground = Color(0xFF3A2830),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF3A2830),
    surfaceVariant = Color(0xFFFBEAF1),
    onSurfaceVariant = Color(0xFF6B5560),
    outline = Color(0xFFA98E9A),
    outlineVariant = Color(0xFFF2DCE6),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF4F8),
    surfaceContainer = Color(0xFFFFEDF3),
    surfaceContainerHigh = Color(0xFFFCE3EC),
    surfaceContainerHighest = Color(0xFFF8D7E4),

    inverseSurface = Color(0xFF3A2830),
    inverseOnSurface = Color(0xFFFFF4F8),
)

/** 昔涟主题里金额用的柔和玫瑰粉，比主色浅一点，但仍然清晰。 */
val PinkMoneyInk = Color(0xFFC95F86)

/** 三种日状态用的语义色，独立于 Material 角色，保证浅色深色都能一眼分辨。 */
data class DayInk(
    val work: Color,
    val rest: Color,
    val leave: Color,
) {
    fun of(status: DayStatus): Color = when (status) {
        DayStatus.work -> work
        DayStatus.rest -> rest
        DayStatus.leave -> leave
    }

    fun tint(status: DayStatus, alpha: Float): Color = of(status).copy(alpha = alpha)
}

val LightDayInk = DayInk(
    work = Color(0xFF6A4C93),
    rest = Color(0xFF2E7D62),
    leave = Color(0xFFB4712A),
)

/** 昔涟主题下工作状态也跟着偏玫瑰粉，休息/请假保留可区分的青绿与琥珀。 */
val PinkDayInk = DayInk(
    work = Color(0xFFB94E78),
    rest = Color(0xFF3E8E74),
    leave = Color(0xFFC08A3E),
)

val DarkDayInk = DayInk(
    work = Color(0xFFC9AEFF),
    rest = Color(0xFF7FD3B2),
    leave = Color(0xFFF0B872),
)

val LocalDayInk = staticCompositionLocalOf { LightDayInk }

/** 支出分类色，按分类名取色；自定义分类回落到「其他」。 */
data class ExpenseInk(private val colors: Map<String, Color>) {
    fun of(name: String?): Color = colors[name] ?: colors.getValue("其他")
}

val LightExpenseInk = ExpenseInk(
    mapOf(
        "餐饮" to Color(0xFFB0447C),
        "交通" to Color(0xFF3F63B5),
        "购物" to Color(0xFFB4712A),
        "居住" to Color(0xFF2E7D62),
        "娱乐" to Color(0xFF7E57A8),
        "医疗" to Color(0xFFB3453E),
        "其他" to Color(0xFF6F6A75),
    ),
)

val DarkExpenseInk = ExpenseInk(
    mapOf(
        "餐饮" to Color(0xFFFFA8CF),
        "交通" to Color(0xFFA8C0FF),
        "购物" to Color(0xFFF0B872),
        "居住" to Color(0xFF7FD3B2),
        "娱乐" to Color(0xFFD4B5F0),
        "医疗" to Color(0xFFFFB4AB),
        "其他" to Color(0xFFCAC4CF),
    ),
)

val LocalExpenseInk = staticCompositionLocalOf { LightExpenseInk }

fun statusLabel(status: DayStatus): String = when (status) {
    DayStatus.work -> "工作"
    DayStatus.rest -> "休息"
    DayStatus.leave -> "请假"
}

fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.system -> "跟随系统"
    ThemeMode.light -> "浅色"
    ThemeMode.dark -> "深色"
    ThemeMode.pink -> "昔涟"
}
