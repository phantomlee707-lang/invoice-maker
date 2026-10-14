package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic, decimal-safe invoice math. All money stays in integer minor
 * units; the only fractional step (tax) rounds HALF_UP to a minor unit so the
 * same inputs always produce the same totals.
 */
object InvoiceCalculator {

    fun lineTotal(item: LineItemInput): LineTotal {
        val gross = item.unitPrice * item.quantity
        // Cap the discount at the line gross so a line can never go negative.
        val discount = if (item.discount.minor > gross.minor) gross else item.discount
        val taxable = gross - discount

        val tax = item.taxPercent?.let { pct ->
            val raw = taxable.count().multiply(pct).divide(BigDecimal(100))
            Money.of(raw.setScale(0, RoundingMode.HALF_UP).toLong())
        } ?: Money.ZERO

        return LineTotal(
            gross = gross,
            discount = discount,
            taxable = taxable,
            tax = tax,
            total = taxable + tax,
        )
    }

    fun totals(items: List<LineItemInput>): InvoiceTotals {
        var subtotal = 0L
        var totalDiscount = 0L
        var totalTax = 0L
        for (item in items) {
            val lt = lineTotal(item)
            subtotal += lt.gross.minor
            totalDiscount += lt.discount.minor
            totalTax += lt.tax.minor
        }
        val grandTotal = subtotal - totalDiscount + totalTax
        return InvoiceTotals(
            subtotal = Money.of(subtotal),
            totalDiscount = Money.of(totalDiscount),
            totalTax = Money.of(totalTax),
            grandTotal = Money.of(grandTotal),
        )
    }
}
