package com.worktime.app.data

/** 旧 JSON / 备份里的分类标识与新分类名之间的映射。 */
object CategoryMapping {

    private val legacyToName = mapOf(
        "food" to "餐饮",
        "transport" to "交通",
        "shopping" to "购物",
        "home" to "居住",
        "entertainment" to "娱乐",
        "health" to "医疗",
        "other" to "其他",
    )

    private val nameToLegacy = legacyToName.entries.associate { (legacy, name) -> name to legacy }

    /** 旧 JSON 的 category（如 shopping）→ 数据库分类名；无法识别时按原样创建。 */
    fun nameForLegacy(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return legacyToName[value.lowercase()] ?: value
    }

    /** 数据库分类名 → 导出 JSON 的 category；自定义分类原样输出。 */
    fun legacyForName(name: String?): String? {
        val value = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return nameToLegacy[value] ?: value
    }
}
