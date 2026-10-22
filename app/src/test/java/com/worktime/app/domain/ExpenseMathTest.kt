package com.worktime.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseMathTest {

    @Test
    fun parseAmounts() {
        assertEquals(1200L, ExpenseMath.parseAmountToCents("12"))
        assertEquals(1230L, ExpenseMath.parseAmountToCents("12.3"))
        assertEquals(1234L, ExpenseMath.parseAmountToCents("12.34"))
        assertEquals(1L, ExpenseMath.parseAmountToCents("0.01"))
        assertEquals(850L, ExpenseMath.parseAmountToCents(" 8.50 "))
    }

    @Test
    fun rejectInvalidAmounts() {
        listOf("", " ", "0", "0.00", "-1", "1.234", "abc", "1..2", ".5", "10,00")
            .forEach { assertNull(it, ExpenseMath.parseAmountToCents(it)) }
    }

    @Test
    fun rejectAmountsOverTheLimit() {
        assertNull(ExpenseMath.parseAmountToCents("10000000.01"))
    }

    @Test
    fun formatAmounts() {
        assertEquals("12.34", ExpenseMath.formatCents(1234))
        assertEquals("0.05", ExpenseMath.formatCents(5))
        assertEquals("1,000.00", ExpenseMath.formatCents(100_000))
        assertEquals("12.34", ExpenseMath.formatPlain(1234))
        assertEquals("0.05", ExpenseMath.formatPlain(5))
    }
}
