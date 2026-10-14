package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceCalculatorTest {

    private fun money(minor: Long) = Money.of(minor)

    @Test fun simpleLineNoTax() {
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "Widget", quantity = BigDecimal("2"), unitPrice = money(500))
        )
        assertEquals(1000L, lt.gross.minor)
        assertEquals(0L, lt.tax.minor)
        assertEquals(1000L, lt.total.minor)
    }

    @Test fun fractionalQuantity() {
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "Labor", quantity = BigDecimal("1.5"), unitPrice = money(4000))
        )
        assertEquals(6000L, lt.total.minor)
    }

    @Test fun discountCappedAtGross() {
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "X", quantity = BigDecimal.ONE, unitPrice = money(500), discount = money(9999))
        )
        assertEquals(500L, lt.discount.minor) // capped at gross
        assertEquals(0L, lt.total.minor)
    }

    @Test fun taxRoundsHalfUpDown() {
        // taxable 1050 * 8.5% = 89.25 -> 89
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "X", quantity = BigDecimal.ONE, unitPrice = money(1050), taxPercent = BigDecimal("8.5"))
        )
        assertEquals(1050L, lt.taxable.minor)
        assertEquals(89L, lt.tax.minor)
        assertEquals(1139L, lt.total.minor)
    }

    @Test fun taxRoundsHalfUpUp() {
        // taxable 100 * 8.5% = 8.5 -> 9 (HALF_UP)
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "X", quantity = BigDecimal.ONE, unitPrice = money(100), taxPercent = BigDecimal("8.5"))
        )
        assertEquals(9L, lt.tax.minor)
    }

    @Test fun totalsAggregate() {
        val items = listOf(
            LineItemInput(description = "A", quantity = BigDecimal("2"), unitPrice = money(1000)),
            LineItemInput(description = "B", quantity = BigDecimal.ONE, unitPrice = money(500), discount = money(100)),
            LineItemInput(description = "C", quantity = BigDecimal.ONE, unitPrice = money(1000), taxPercent = BigDecimal("10")),
        )
        val t = InvoiceCalculator.totals(items)
        assertEquals(3500L, t.subtotal.minor)    // 2000 + 500 + 1000
        assertEquals(100L, t.totalDiscount.minor)
        assertEquals(100L, t.totalTax.minor)     // 1000 * 10%
        assertEquals(3500L, t.grandTotal.minor)  // 3500 - 100 + 100
    }

    @Test fun emptyInvoiceIsZero() {
        val t = InvoiceCalculator.totals(emptyList())
        assertEquals(0L, t.grandTotal.minor)
    }

    @Test fun zeroQuantityLine() {
        val lt = InvoiceCalculator.lineTotal(
            LineItemInput(description = "X", quantity = BigDecimal.ZERO, unitPrice = money(1000))
        )
        assertEquals(0L, lt.total.minor)
    }

    @Test fun negativeInputRejectedByValidator() {
        val errors = InvoiceValidator.validateLine(
            LineItemInput(description = "", quantity = BigDecimal("-1"), unitPrice = money(-5))
        )
        assert(errors.size >= 3) { "expected multiple validation errors, got $errors" }
    }
}
