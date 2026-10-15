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
        assertEquals(1000L, lt.taxable.minor)
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
        // 1050 * 8.5% = 89.25 -> 89
        val tax = InvoiceCalculator.taxAmount(money(1050), money(0), BigDecimal("8.5"))
        assertEquals(89L, tax.minor)
    }

    @Test fun taxRoundsHalfUpUp() {
        // 100 * 8.5% = 8.5 -> 9 (HALF_UP)
        val tax = InvoiceCalculator.taxAmount(money(100), money(0), BigDecimal("8.5"))
        assertEquals(9L, tax.minor)
    }

    @Test fun taxOnDiscountedBase() {
        // subtotal 1050, discount 50 -> base 1000 * 10% = 100
        val tax = InvoiceCalculator.taxAmount(money(1050), money(50), BigDecimal("10"))
        assertEquals(100L, tax.minor)
    }

    @Test fun totalsAggregate() {
        val items = listOf(
            LineItemInput(description = "A", quantity = BigDecimal("2"), unitPrice = money(1000)),
            LineItemInput(description = "B", quantity = BigDecimal.ONE, unitPrice = money(500), discount = money(100)),
            LineItemInput(description = "C", quantity = BigDecimal.ONE, unitPrice = money(1000)),
        )
        val t = InvoiceCalculator.totals(items, BigDecimal("10"))
        assertEquals(3500L, t.subtotal.minor)     // 2000 + 500 + 1000
        assertEquals(100L, t.totalDiscount.minor)
        assertEquals(340L, t.taxAmount.minor)     // (3500 - 100) * 10% = 340
        assertEquals(3740L, t.grandTotal.minor)   // 3500 - 100 + 340
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
