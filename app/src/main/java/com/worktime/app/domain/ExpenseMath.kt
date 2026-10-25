package com.worktime.app.domain

import java.util.Locale

/** 支出金额的解析与格式化，不依赖 Android API。 */
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
}
