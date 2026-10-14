package com.rainweaver.invoicemaker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test fun plusMinus() {
        assertEquals(150L, (Money.of(100) + Money.of(50)).minor)
        assertEquals(50L, (Money.of(100) - Money.of(50)).minor)
    }

    @Test fun timesRoundsHalfUp() {
        assertEquals(101L, (Money.of(100) * java.math.BigDecimal("1.005")).minor) // 100.5 -> 101
        assertEquals(66L, (Money.of(33) * java.math.BigDecimal("2")).minor)
        assertEquals(30L, (Money.of(10) * java.math.BigDecimal("3")).minor)
    }

    @Test fun fromDecimalUsesExponent() {
        assertEquals(1050L, Money.fromDecimal("10.50").minor)
        assertEquals(5L, Money.fromDecimal("0.05").minor)
        assertEquals(0L, Money.fromDecimal("0.00").minor)
        assertEquals(1200L, Money.fromDecimal("1200", exponent = 0).minor) // JPY-style
    }

    @Test fun zeroAndCompare() {
        assertTrue(Money.ZERO < Money.of(1))
        assertEquals(0L, Money.ZERO.minor)
    }
}
