package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal

/** A single invoice line as entered by the user. [id] is a stable identity used as a UI list key. */
data class LineItemInput(
    val id: Long = nextId(),
    val description: String = "",
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPrice: Money = Money.ZERO,
    val discount: Money = Money.ZERO,
) {
    companion object {
        private val seq = java.util.concurrent.atomic.AtomicLong(0)
        fun nextId(): Long = seq.incrementAndGet()
    }
}

/** The computed breakdown for one line. Tax is applied at the invoice level, not per line. */
data class LineTotal(
    val gross: Money,      // quantity * unitPrice
    val discount: Money,   // discount actually applied (capped at gross)
    val taxable: Money,    // gross - discount
    val total: Money,      // = taxable (tax added at invoice level)
)

/** Invoice-level totals. taxAmount = (subtotal - discount) * taxRate/100. */
data class InvoiceTotals(
    val subtotal: Money,        // sum of line gross
    val totalDiscount: Money,   // sum of applied discounts
    val taxRate: BigDecimal,    // percent, e.g. 11
    val taxAmount: Money,       // (subtotal - discount) * taxRate/100
    val grandTotal: Money,      // subtotal - discount + taxAmount
)
