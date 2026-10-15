package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal

/** Input validation returning human-readable messages (empty list = valid). */
object InvoiceValidator {

    fun validateQuantity(q: BigDecimal): String? =
        if (q <= BigDecimal.ZERO) "Quantity must be greater than zero" else null

    fun validateUnitPrice(p: Money): String? =
        if (p.minor < 0) "Unit price cannot be negative" else null

    fun validateTaxRate(rate: BigDecimal): String? =
        if (rate < BigDecimal.ZERO) "Tax rate cannot be negative" else null

    fun validateLine(item: LineItemInput): List<String> {
        val errors = mutableListOf<String>()
        if (item.description.isBlank()) errors += "Description is required"
        validateQuantity(item.quantity)?.let { errors += it }
        validateUnitPrice(item.unitPrice)?.let { errors += it }
        if (item.discount.minor < 0) errors += "Discount cannot be negative"
        return errors
    }
}
