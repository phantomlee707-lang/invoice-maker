package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal

/** A single invoice line as entered by the user. [id] is a stable identity used as a UI list key. */
data class LineItemInput(
    val id: Long = nextId(),
    val description: String = "",
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPrice: Money = Money.ZERO,
    val discount: Money = Money.ZERO,
    val taxPercent: BigDecimal? = null,
) {
    companion object {
        private val seq = java.util.concurrent.atomic.AtomicLong(0)
        fun nextId(): Long = seq.incrementAndGet()
    }
}

/** The computed breakdown for one line. */
data class LineTotal(
    val gross: Money,      // quantity * unitPrice
    val discount: Money,   // discount actually applied (capped at gross)
    val taxable: Money,    // gross - discount
    val tax: Money,        // taxable * taxPercent/100 (HALF_UP)
    val total: Money,      // taxable + tax
)

/** Invoice-level totals. */
data class InvoiceTotals(
    val subtotal: Money,        // sum of line gross
    val totalDiscount: Money,   // sum of applied discounts
    val totalTax: Money,        // sum of line taxes
    val grandTotal: Money,      // subtotal - totalDiscount + totalTax
)
