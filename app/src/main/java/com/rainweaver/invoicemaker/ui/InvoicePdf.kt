package com.rainweaver.invoicemaker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.rainweaver.invoicemaker.data.local.BusinessProfileEntity
import com.rainweaver.invoicemaker.data.local.InvoiceLineEntity
import com.rainweaver.invoicemaker.data.local.InvoiceWithLines
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * On-device PDF rendering with three templates (Minimal, Classic, Modern).
 * Paginates long invoices; writes to cacheDir/invoices/<number>.pdf.
 */
object InvoicePdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val RIGHT = PAGE_WIDTH - MARGIN

    private data class Style(
        val accent: Int,
        val headerFill: Int?,   // null = no fill
        val ruleColor: Int,
        val headerText: Int,
    )

    private fun styleFor(template: String): Style = when (template.uppercase()) {
        "CLASSIC" -> Style(accent = Color.rgb(0x1F, 0x2D, 0x3D), headerFill = Color.rgb(0xEC, 0xEF, 0xF1), ruleColor = Color.rgb(0x1F, 0x2D, 0x3D), headerText = Color.rgb(0x1F, 0x2D, 0x3D))
        "MODERN" -> Style(accent = Color.rgb(0x00, 0x89, 0x7B), headerFill = Color.rgb(0x00, 0x89, 0x7B), ruleColor = Color.rgb(0xCF, 0xD8, 0xDC), headerText = Color.WHITE)
        else -> Style(accent = Color.rgb(0x21, 0x21, 0x21), headerFill = null, ruleColor = Color.rgb(0x9E, 0x9E, 0x9E), headerText = Color.rgb(0x21, 0x21, 0x21))
    }

    private fun plain(minor: Long, digits: Int): String {
        val amount = BigDecimal(minor).movePointLeft(digits)
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return nf.format(amount)
    }

    private fun sym(currency: String): String {
        val c = runCatching { Currency.getInstance(currency.trim()) }.getOrNull()
        return c?.getSymbol(Locale.US) ?: currency.trim().let { if (it.isEmpty()) "" else "$it " }
    }

    /** With currency symbol — used only for the grand total. */
    private fun money(minor: Long, currency: String, digits: Int): String = sym(currency) + plain(minor, digits)

    fun writePdf(context: Context, inv: InvoiceWithLines, profile: BusinessProfileEntity, template: String): File {
        val doc = PdfDocument()
        val style = styleFor(template)
        val currency = inv.invoice.currency
        val digits = runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrElse { 2 }.coerceAtLeast(0)

        var pageNumber = 1
        var info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = doc.startPage(info)
        var canvas = page.canvas
        var y = drawHeader(canvas, inv, profile, style)

        y = drawTableHeader(canvas, y, style)
        for (line in inv.lines) {
            if (y > PAGE_HEIGHT - MARGIN - 140) {
                drawFooter(canvas, pageNumber)
                doc.finishPage(page)
                pageNumber++
                info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = doc.startPage(info)
                canvas = page.canvas
                y = drawTableHeader(canvas, MARGIN + 10, style)
            }
            y = drawLineRow(canvas, y, line, digits)
        }

        y += 12
        y = drawTotals(canvas, y, inv, currency, digits, style)
        drawNotes(canvas, y, inv, style)
        drawFooter(canvas, pageNumber)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safe = inv.invoice.number.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, "$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawHeader(c: Canvas, inv: InvoiceWithLines, profile: BusinessProfileEntity, s: Style): Float {
        var y = MARGIN + 10
        val title = Paint().apply { textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = s.accent }
        c.drawText(profile.name.ifBlank { "Your Business" }, MARGIN, y, title)
        y += 6
        val sub = Paint().apply { textSize = 9f; color = Color.rgb(0x60, 0x60, 0x60) }
        val contact = listOf(profile.address, profile.phone, profile.email).filter { it.isNotBlank() }.joinToString("  •  ")
        if (contact.isNotBlank()) { c.drawText(contact, MARGIN, y + 12, sub); y += 14 }

        // "INVOICE" + number on the right
        val invPaint = Paint().apply { textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = s.accent; textAlign = Paint.Align.RIGHT }
        c.drawText("INVOICE", RIGHT, MARGIN + 10, invPaint)
        val numPaint = Paint().apply { textSize = 11f; color = Color.rgb(0x40, 0x40, 0x40); textAlign = Paint.Align.RIGHT }
        c.drawText(inv.invoice.number, RIGHT, MARGIN + 26, numPaint)
        c.drawText("Issued ${fmtDate(inv.invoice.issueDate)}", RIGHT, MARGIN + 42, numPaint)
        inv.invoice.dueDate?.let { c.drawText("Due ${fmtDate(it)}", RIGHT, MARGIN + 56, numPaint) }

        y = (MARGIN + 70).coerceAtLeast(y + 20)

        // Bill-to
        val billLabel = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = s.accent }
        c.drawText("BILL TO", MARGIN, y, billLabel)
        y += 14
        val bill = Paint().apply { textSize = 11f; color = Color.rgb(0x20, 0x20, 0x20) }
        c.drawText(inv.invoice.customerName.ifBlank { "Customer" }, MARGIN, y, bill); y += 13
        listOf(inv.invoice.customerAddress, inv.invoice.customerEmail, inv.invoice.customerPhone)
            .filter { it.isNotBlank() }.forEach { c.drawText(it, MARGIN, y, bill); y += 13 }

        y += 10
        val rule = Paint().apply { color = s.ruleColor; strokeWidth = 1f }
        c.drawLine(MARGIN, y, RIGHT, y, rule)
        return y + 18
    }

    private fun drawTableHeader(c: Canvas, yStart: Float, s: Style): Float {
        val h = 22f
        s.headerFill?.let { fill ->
            val p = Paint().apply { color = fill }
            c.drawRect(MARGIN, yStart, RIGHT, yStart + h, p)
        }
        val th = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = s.headerText }
        c.drawText("DESCRIPTION", MARGIN + 6, yStart + 15, th)
        c.drawText("QTY", 330f, yStart + 15, th)
        c.drawText("UNIT", 385f, yStart + 15, th)
        c.drawText("AMOUNT", RIGHT - 6, yStart + 15, Paint(th).apply { textAlign = Paint.Align.RIGHT })
        val rule = Paint().apply { color = s.ruleColor; strokeWidth = 1f }
        c.drawLine(MARGIN, yStart + h, RIGHT, yStart + h, rule)
        return yStart + h + 16
    }

    private fun drawLineRow(c: Canvas, yStart: Float, line: InvoiceLineEntity, digits: Int): Float {
        val desc = Paint().apply { textSize = 11f; color = Color.rgb(0x20, 0x20, 0x20) }
        val maxChars = 52
        val text = line.description.ifBlank { "(no description)" }
        var y = yStart
        for (ln in wrapText(text, maxChars)) {
            c.drawText(ln, MARGIN + 6, y, desc)
            y += 15
        }
        val cell = Paint().apply { textSize = 11f; color = Color.rgb(0x20, 0x20, 0x20) }
        val qty = runCatching { java.math.BigDecimal(line.quantity).stripTrailingZeros().toPlainString() }.getOrElse { line.quantity }
        c.drawText(qty, 330f, yStart, cell)
        c.drawText(plain(line.unitPriceMinor, digits), 385f, yStart, cell)
        c.drawText(plain(line.lineTotalMinor, digits), RIGHT - 6, yStart, Paint(cell).apply { textAlign = Paint.Align.RIGHT })
        // light separator under the row, then a comfortable gap before the next
        y += 6
        val sep = Paint().apply { color = Color.rgb(0xE3, 0xE6, 0xE8); strokeWidth = 0.8f }
        c.drawLine(MARGIN, y, RIGHT, y, sep)
        return y + 11
    }

    /** Greedy word-wrap into lines of at most [maxChars]; hard-splits overlong words. */
    private fun wrapText(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in text.split(" ")) {
            val candidate = if (cur.isEmpty()) w else "$cur $w"
            if (candidate.length <= maxChars) {
                cur = StringBuilder(candidate)
            } else {
                if (cur.isNotEmpty()) { lines.add(cur.toString()); cur = StringBuilder() }
                if (w.length > maxChars) {
                    var i = 0
                    while (i < w.length) {
                        val end = (i + maxChars).coerceAtMost(w.length)
                        lines.add(w.substring(i, end)); i = end
                    }
                } else {
                    cur = StringBuilder(w)
                }
            }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines
    }

    private fun drawTotals(c: Canvas, yStart: Float, inv: InvoiceWithLines, currency: String, digits: Int, s: Style): Float {
        var y = yStart
        val label = Paint().apply { textSize = 11f; color = Color.rgb(0x40, 0x40, 0x40) }
        val value = Paint().apply { textSize = 11f; color = Color.rgb(0x20, 0x20, 0x20); textAlign = Paint.Align.RIGHT }
        fun row(l: String, v: String) {
            c.drawText(l, 360f, y, label)
            c.drawText(v, RIGHT - 6, y, value)
            y += 16
        }
        row("Subtotal", plain(inv.invoice.subtotalMinor, digits))
        if (inv.invoice.totalDiscountMinor != 0L) row("Discount", "-" + plain(inv.invoice.totalDiscountMinor, digits))
        if (inv.invoice.totalTaxMinor != 0L) row("Tax", plain(inv.invoice.totalTaxMinor, digits))
        y += 2
        val rule = Paint().apply { color = s.ruleColor; strokeWidth = 1f }
        c.drawLine(340f, y, RIGHT, y, rule)
        y += 18
        val gl = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = s.accent }
        val gv = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = s.accent; textAlign = Paint.Align.RIGHT }
        c.drawText("TOTAL DUE", 340f, y, gl)
        c.drawText(money(inv.invoice.grandTotalMinor, currency, digits), RIGHT - 6, y, gv)
        return y + 20
    }

    private fun drawNotes(c: Canvas, yStart: Float, inv: InvoiceWithLines, s: Style) {
        var y = yStart
        val label = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = s.accent }
        val body = Paint().apply { textSize = 10f; color = Color.rgb(0x40, 0x40, 0x40) }
        if (inv.invoice.businessPaymentInstructions.isNotBlank()) {
            c.drawText("PAYMENT INSTRUCTIONS", MARGIN, y, label); y += 13
            c.drawText(inv.invoice.businessPaymentInstructions, MARGIN, y, body); y += 18
        }
        if (inv.invoice.notes.isNotBlank()) {
            c.drawText("NOTES", MARGIN, y, label); y += 13
            c.drawText(inv.invoice.notes, MARGIN, y, body); y += 18
        }
        if (inv.invoice.terms.isNotBlank()) {
            c.drawText("TERMS", MARGIN, y, label); y += 13
            c.drawText(inv.invoice.terms, MARGIN, y, body)
        }
    }

    private fun drawFooter(c: Canvas, pageNumber: Int) {
        val f = Paint().apply { textSize = 8f; color = Color.rgb(0x90, 0x90, 0x90); textAlign = Paint.Align.CENTER }
        c.drawText("Rainweaver Studio  •  Invoice Maker  •  Page $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, f)
    }
}
