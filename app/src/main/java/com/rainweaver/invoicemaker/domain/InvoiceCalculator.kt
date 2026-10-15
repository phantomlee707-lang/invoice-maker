package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic, decimal-safe invoice math. All money stays in integer minor
 * units; the only fractional step (tax) rounds HALF_UP to a minor unit so the
 * same inputs always produce the same totals. Tax is invoice-level.
 */
object InvoiceCalculator {

    fun lineTotal(item: LineItemInput): LineTotal {
        val gross = item.unitPrice * item.quantity
        // Cap the discount at the line gross so a line can never go negative.
        val discount = if (item.discount.minor > gross.minor) gross else item.discount
        val taxable = gross - discount
        return LineTotal(gross = gross, discount = discount, taxable = taxable, total = taxable)
    }

    /** Invoice-level tax on the discounted base: (subtotal - discount) * rate/100, HALF_UP. */
    fun taxAmount(subtotal: Money, discount: Money, ratePercent: BigDecimal): Money {
        if (ratePercent.signum() == 0) return Money.ZERO
        val base = subtotal - discount
        val raw = base.count().multiply(ratePercent).divide(BigDecimal(100))
        return Money.of(raw.setScale(0, RoundingMode.HALF_UP).toLong())
    }

    fun totals(items: List<LineItemInput>, taxRate: BigDecimal = BigDecimal.ZERO): InvoiceTotals {
        var subtotal = 0L
        var totalDiscount = 0L
        for (item in items) {
            val lt = lineTotal(item)
            subtotal += lt.gross.minor
            totalDiscount += lt.discount.minor
        }
        val tax = taxAmount(Money.of(subtotal), Money.of(totalDiscount), taxRate)
        val grandTotal = subtotal - totalDiscount + tax.minor
        return InvoiceTotals(
            subtotal = Money.of(subtotal),
            totalDiscount = Money.of(totalDiscount),
            taxRate = taxRate,
            taxAmount = tax,
            grandTotal = Money.of(grandTotal),
        )
    }
}
