package com.rainweaver.invoicemaker.ui

import com.rainweaver.invoicemaker.domain.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Format integer minor units as a localized currency string (exponent-aware). */
fun Money.format(currencyCode: String, locale: Locale = Locale.US): String {
    val code = currencyCode.trim()
    // Accept either an ISO code (USD, IDR, EUR → proper symbol + fraction digits)
    // or a literal symbol/prefix the user typed (Rp, €, $, ₱).
    val currency = runCatching { Currency.getInstance(code) }.getOrNull()
    val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
    val symbol = currency?.getSymbol(locale) ?: if (code.isEmpty()) "" else "$code "
    val amount = BigDecimal(minor).movePointLeft(digits)
    val nf = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    return "$symbol${nf.format(amount)}"
}
