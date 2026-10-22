package com.worktime.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryMappingTest {

    @Test
    fun mapsLegacyIdentifiersToNames() {
        assertEquals("餐饮", CategoryMapping.nameForLegacy("food"))
        assertEquals("交通", CategoryMapping.nameForLegacy("transport"))
        assertEquals("购物", CategoryMapping.nameForLegacy("shopping"))
        assertEquals("居住", CategoryMapping.nameForLegacy("home"))
        assertEquals("娱乐", CategoryMapping.nameForLegacy("entertainment"))
        assertEquals("医疗", CategoryMapping.nameForLegacy("health"))
        assertEquals("其他", CategoryMapping.nameForLegacy("other"))
    }

    @Test
    fun keepsUnknownIdentifiersAsNames() {
        assertEquals("咖啡", CategoryMapping.nameForLegacy("咖啡"))
        assertEquals("pet", CategoryMapping.nameForLegacy("pet"))
    }

    @Test
    fun missingCategoryStaysNull() {
        assertNull(CategoryMapping.nameForLegacy(null))
        assertNull(CategoryMapping.nameForLegacy("  "))
    }

    @Test
    fun mapsNamesBackToLegacyIdentifiersForExport() {
        assertEquals("food", CategoryMapping.legacyForName("餐饮"))
        assertEquals("shopping", CategoryMapping.legacyForName("购物"))
        assertEquals("咖啡", CategoryMapping.legacyForName("咖啡"))
        assertNull(CategoryMapping.legacyForName(null))
    }
}
