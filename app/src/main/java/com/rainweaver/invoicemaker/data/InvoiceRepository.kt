package com.rainweaver.invoicemaker.data

import com.rainweaver.invoicemaker.data.local.AppSettingsEntity
import com.rainweaver.invoicemaker.data.local.BusinessProfileEntity
import com.rainweaver.invoicemaker.data.local.CatalogItemEntity
import com.rainweaver.invoicemaker.data.local.CustomerEntity
import com.rainweaver.invoicemaker.data.local.InvoiceDatabase
import com.rainweaver.invoicemaker.data.local.InvoiceEntity
import com.rainweaver.invoicemaker.data.local.InvoiceLineEntity
import com.rainweaver.invoicemaker.data.local.InvoiceWithLines
import com.rainweaver.invoicemaker.domain.InvoiceCalculator
import com.rainweaver.invoicemaker.domain.InvoiceNumbering
import com.rainweaver.invoicemaker.domain.InvoiceStatus
import com.rainweaver.invoicemaker.domain.LineItemInput

/**
 * Single entry point for persistence. Creating an invoice computes totals,
 * assigns a unique number, and freezes customer/business/line snapshots so an
 * issued invoice is reproducible forever, independent of later edits.
 */
class InvoiceRepository(private val db: InvoiceDatabase) {

    private val profileDao = db.businessProfileDao()
    private val customerDao = db.customerDao()
    private val catalogDao = db.catalogItemDao()
    private val invoiceDao = db.invoiceDao()
    private val settingsDao = db.appSettingsDao()

    // ---- business profile ----
    suspend fun businessProfile(): BusinessProfileEntity =
        profileDao.get() ?: BusinessProfileEntity().also { profileDao.upsert(it) }

    suspend fun saveBusinessProfile(p: BusinessProfileEntity) =
        profileDao.upsert(p.copy(updatedAt = System.currentTimeMillis()))

    // ---- customers ----
    suspend fun customers() = customerDao.getAll()
    suspend fun searchCustomers(q: String) = customerDao.search(q)
    suspend fun customer(id: Long) = customerDao.getById(id)
    suspend fun saveCustomer(c: CustomerEntity): Long =
        if (c.id == 0L) customerDao.insert(c) else { customerDao.update(c); c.id }
    suspend fun deleteCustomer(id: Long) = customerDao.deleteById(id)

    // ---- catalog ----
    suspend fun catalog() = catalogDao.getAll()
    suspend fun searchCatalog(q: String) = catalogDao.search(q)
    suspend fun saveCatalogItem(i: CatalogItemEntity): Long =
        if (i.id == 0L) catalogDao.insert(i) else { catalogDao.update(i); i.id }
    suspend fun deleteCatalogItem(id: Long) = catalogDao.deleteById(id)

    // ---- settings ----
    private suspend fun settings(): AppSettingsEntity =
        settingsDao.get() ?: AppSettingsEntity().also { settingsDao.upsert(it) }

    suspend fun appSettings() = settings()

    // ---- invoices ----
    suspend fun invoices(): List<InvoiceWithLines> = invoiceDao.getAllWithLines()
    suspend fun invoice(id: Long): InvoiceWithLines? = invoiceDao.getByIdWithLines(id)

    suspend fun createInvoice(
        customer: CustomerEntity?,
        customerOneOff: CustomerEntity?,
        lines: List<LineItemInput>,
        issueDate: Long,          // epochDay
        dueDate: Long?,           // epochDay
        notes: String,
        terms: String,
        template: String,
        status: InvoiceStatus = InvoiceStatus.ISSUED,
    ): Long {
        val biz = businessProfile()
        val cust = customer ?: customerOneOff ?: CustomerEntity(name = "")
        val number = assignNumber()
        val totals = InvoiceCalculator.totals(lines)

        val header = InvoiceEntity(
            number = number,
            issueDate = issueDate,
            dueDate = dueDate,
            status = status.name,
            currency = biz.currency,
            template = template,
            customerName = cust.name,
            customerContact = cust.contactPerson,
            customerAddress = cust.address,
            customerEmail = cust.email,
            customerPhone = cust.phone,
            businessName = biz.name,
            businessAddress = biz.address,
            businessPhone = biz.phone,
            businessEmail = biz.email,
            businessPaymentInstructions = biz.paymentInstructions,
            subtotalMinor = totals.subtotal.minor,
            totalDiscountMinor = totals.totalDiscount.minor,
            totalTaxMinor = totals.totalTax.minor,
            grandTotalMinor = totals.grandTotal.minor,
            notes = notes,
            terms = terms,
            createdAt = System.currentTimeMillis(),
        )

        val lineEntities = lines.map { item ->
            val lt = InvoiceCalculator.lineTotal(item)
            InvoiceLineEntity(
                invoiceId = 0,
                description = item.description,
                quantity = item.quantity.toPlainString(),
                unitPriceMinor = item.unitPrice.minor,
                discountMinor = item.discount.minor,
                taxPercent = item.taxPercent?.toPlainString(),
                lineGrossMinor = lt.gross.minor,
                lineDiscountMinor = lt.discount.minor,
                lineTaxMinor = lt.tax.minor,
                lineTotalMinor = lt.total.minor,
            )
        }

        return invoiceDao.insertInvoiceWithLines(header, lineEntities)
    }

    /** Assign a unique invoice number in YYYYMMDDHHmmss format. */
    private suspend fun assignNumber(): String {
        var millis = System.currentTimeMillis()
        var candidate = InvoiceNumbering.timestamp(millis)
        // Guarantee uniqueness if two invoices are created within the same second.
        while (invoiceDao.findByNumber(candidate) != null) {
            millis += 1000
            candidate = InvoiceNumbering.timestamp(millis)
        }
        return candidate
    }

    suspend fun markPaid(id: Long) = invoiceDao.updateStatus(id, InvoiceStatus.PAID.name)
    suspend fun deleteInvoice(id: Long) = invoiceDao.deleteById(id)
}
