package com.rainweaver.invoicemaker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is stored as integer minor units (cents, etc.) — never binary
 * floating point. All arithmetic that can produce a fractional minor unit
 * rounds HALF_UP to the nearest minor unit, deterministically.
 *
 * The type is currency-agnostic: it holds a count of the smallest currency
 * unit. The decimal exponent (2 for USD, 0 for JPY, …) is applied only at
 * parse/format time, so calculations stay correct for any currency.
 */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minor + other.minor)

    operator fun minus(other: Money): Money = Money(minor - other.minor)

    /** Multiply by a (possibly fractional) quantity, rounding HALF_UP to minor units. */
    operator fun times(quantity: BigDecimal): Money =
        Money(BigDecimal(minor).multiply(quantity).setScale(0, RoundingMode.HALF_UP).toLong())

    /** The raw minor-unit count as a BigDecimal (for percentage math). */
    fun count(): BigDecimal = BigDecimal(minor)

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    companion object {
        fun of(minor: Long): Money = Money(minor)
        val ZERO: Money = Money(0)

        /** Parse a decimal string like "10.50" using the given minor-unit exponent (default 2). */
        fun fromDecimal(text: String, exponent: Int = 2): Money {
            val amount = BigDecimal(text.trim())
            return Money(amount.movePointRight(exponent).setScale(0, RoundingMode.HALF_UP).toLong())
        }
    }
}
