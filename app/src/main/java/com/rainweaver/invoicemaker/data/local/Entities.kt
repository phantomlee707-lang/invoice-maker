package com.rainweaver.invoicemaker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Single-row business identity + defaults. */
@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val contactName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logoUri: String? = null,
    val currency: String = "USD",
    val locale: String = "en_US",
    val paymentInstructions: String = "",
    val updatedAt: Long = 0L,
)

/** Reusable customer record. */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val address: String = "",
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
)

/** Reusable product/service. defaultTaxPercent stored as a plain string (BigDecimal). */
@Entity(tableName = "catalog_items")
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val unitPriceMinor: Long,
    val unit: String = "",
    val defaultTaxPercent: String? = null,
)

/**
 * Invoice header + lifecycle. Customer and business details are FROZEN here as
 * columns (snapshots) so editing those records later never changes an issued
 * invoice. Totals are also snapshotted at issue time.
 */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val issueDate: Long,            // epochDay
    val dueDate: Long? = null,      // epochDay
    val status: String,             // DRAFT / ISSUED / PAID
    val currency: String,
    val template: String = "MINIMAL",
    // frozen customer snapshot
    val customerName: String,
    val customerContact: String = "",
    val customerAddress: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    // frozen business snapshot
    val businessName: String,
    val businessAddress: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessPaymentInstructions: String = "",
    // frozen totals
    val subtotalMinor: Long,
    val totalDiscountMinor: Long,
    val totalTaxMinor: Long,
    val grandTotalMinor: Long,
    val notes: String = "",
    val terms: String = "",
    val createdAt: Long = 0L,
)

/**
 * Invoice line snapshot. description/price/discount/tax are frozen values, not
 * references to the catalog, so historical invoices are reproducible.
 */
@Entity(
    tableName = "invoice_lines",
    foreignKeys = [ForeignKey(
        entity = InvoiceEntity::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("invoiceId")],
)
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val description: String,
    val quantity: String,           // BigDecimal as plain string
    val unitPriceMinor: Long,
    val discountMinor: Long,
    val taxPercent: String?,        // BigDecimal as plain string
    // computed snapshot
    val lineGrossMinor: Long,
    val lineDiscountMinor: Long,
    val lineTaxMinor: Long,
    val lineTotalMinor: Long,
)

/** Small local preferences + numbering state (single row). */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val invoicePrefix: String = "INV-",
    val lastSequence: Long = 0,
    val padding: Int = 4,
    val selectedTemplate: String = "MINIMAL",
    val currency: String = "USD",
    val locale: String = "en_US",
)
