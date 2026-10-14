package com.rainweaver.invoicemaker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceNumberingTest {

    @Test fun nextIncrementsAndPads() {
        assertEquals("INV-0001", InvoiceNumbering.next("INV-", 0))
        assertEquals("INV-0002", InvoiceNumbering.next("INV-", 1))
        assertEquals("INV-1000", InvoiceNumbering.next("INV-", 999))
    }

    @Test fun customPadding() {
        assertEquals("INV-1", InvoiceNumbering.next("INV-", 0, padding = 1))
    }

    @Test fun parseSequenceRoundTrips() {
        assertEquals(42L, InvoiceNumbering.parseSequence("INV-0042", "INV-")!!)
        assertNull(InvoiceNumbering.parseSequence("XXX-0042", "INV-"))
    }

    @Test fun timestampIs14DigitYYYYMMDDHHmmss() {
        val n = InvoiceNumbering.timestamp(0L)
        assertTrue("expected 14 digits, got '$n'", n.matches(Regex("\\d{14}")))
    }
}
