package com.rainweaver.invoicemaker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainweaver.invoicemaker.data.InvoiceRepository
import com.rainweaver.invoicemaker.data.local.BusinessProfileEntity
import com.rainweaver.invoicemaker.data.local.CatalogItemEntity
import com.rainweaver.invoicemaker.data.local.CustomerEntity
import com.rainweaver.invoicemaker.data.local.InvoiceDatabase
import com.rainweaver.invoicemaker.data.local.InvoiceWithLines
import com.rainweaver.invoicemaker.domain.InvoiceCalculator
import com.rainweaver.invoicemaker.domain.InvoiceStatus
import com.rainweaver.invoicemaker.domain.InvoiceTotals
import com.rainweaver.invoicemaker.domain.InvoiceValidator
import com.rainweaver.invoicemaker.domain.LineItemInput
import com.rainweaver.invoicemaker.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal

data class UiState(
    val loaded: Boolean = false,
    val profile: BusinessProfileEntity = BusinessProfileEntity(),
    val customers: List<CustomerEntity> = emptyList(),
    val items: List<CatalogItemEntity> = emptyList(),
    val invoices: List<InvoiceWithLines> = emptyList(),
    val draftCustomerId: Long? = null,
    val draftOneOff: CustomerEntity = CustomerEntity(name = ""),
    val draftLines: List<LineItemInput> = listOf(LineItemInput()),
    val draftIssueDate: Long = todayEpochDay(),
    val draftDueDate: Long? = null,
    val draftNotes: String = "",
    val draftTerms: String = "",
    val draftTemplate: String = "MINIMAL",
    val draftTaxRate: String = "0",
    val message: String? = null,
) {
    val draftTotals: InvoiceTotals get() = InvoiceCalculator.totals(draftLines, draftTaxRate.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    val draftCustomer: CustomerEntity? get() = draftCustomerId?.let { id -> customers.firstOrNull { it.id == id } }
}

class InvoiceViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = InvoiceRepository(InvoiceDatabase.get(app))
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { reload() }

    fun reload() = viewModelScope.launch {
        _state.update {
            it.copy(
                loaded = true,
                profile = repo.businessProfile(),
                customers = repo.customers(),
                items = repo.catalog(),
                invoices = repo.invoices(),
            )
        }
    }

    // ---- business profile ----
    fun saveProfile(p: BusinessProfileEntity) = viewModelScope.launch { repo.saveBusinessProfile(p); reload(); msg("Profile saved") }

    // ---- customers ----
    fun saveCustomer(c: CustomerEntity) = viewModelScope.launch { repo.saveCustomer(c); reload(); msg("Customer saved") }
    fun deleteCustomer(id: Long) = viewModelScope.launch { repo.deleteCustomer(id); reload(); msg("Customer deleted") }

    // ---- catalog ----
    fun saveItem(i: CatalogItemEntity) = viewModelScope.launch { repo.saveCatalogItem(i); reload(); msg("Item saved") }
    fun deleteItem(id: Long) = viewModelScope.launch { repo.deleteCatalogItem(id); reload(); msg("Item deleted") }

    // ---- draft ----
    fun setDraftCustomer(id: Long?) = _state.update { it.copy(draftCustomerId = id) }
    fun setDraftOneOff(c: CustomerEntity) = _state.update { it.copy(draftOneOff = c, draftCustomerId = null) }
    fun setDraftLine(id: Long, line: LineItemInput) =
        _state.update { s -> s.copy(draftLines = s.draftLines.map { if (it.id == id) line else it }) }
    fun addDraftLine() = _state.update { it.copy(draftLines = it.draftLines + LineItemInput()) }
    fun addCatalogLine(description: String, unitPriceMinor: Long) =
        _state.update { it.copy(draftLines = it.draftLines + LineItemInput(description = description, unitPrice = Money.of(unitPriceMinor))) }
    fun removeDraftLine(id: Long) =
        _state.update { s -> if (s.draftLines.size > 1) s.copy(draftLines = s.draftLines.filterNot { it.id == id }) else s }
    fun setDraftDates(issue: Long, due: Long?) = _state.update { it.copy(draftIssueDate = issue, draftDueDate = due) }
    fun setDraftNotes(n: String) = _state.update { it.copy(draftNotes = n) }
    fun setDraftTerms(t: String) = _state.update { it.copy(draftTerms = t) }
    fun setDraftTemplate(t: String) = _state.update { it.copy(draftTemplate = t) }
    fun setDraftTaxRate(t: String) = _state.update { it.copy(draftTaxRate = t) }
    fun clearDraft() = _state.update {
        it.copy(draftCustomerId = null, draftOneOff = CustomerEntity(name = ""), draftLines = listOf(LineItemInput()),
            draftIssueDate = todayEpochDay(), draftDueDate = null, draftNotes = "", draftTerms = "", draftTemplate = "MINIMAL", draftTaxRate = "0")
    }

    // ---- save invoice (validate first) ----
    fun saveInvoice(status: InvoiceStatus, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val s = _state.value
        if (s.draftLines.isEmpty()) { msg("Add at least one line item"); return@launch }
        for (line in s.draftLines) {
            val errs = InvoiceValidator.validateLine(line)
            if (errs.isNotEmpty()) { msg(errs.first()); return@launch }
        }
        val id = repo.createInvoice(
            customer = s.draftCustomer,
            customerOneOff = if (s.draftCustomerId == null) s.draftOneOff else null,
            lines = s.draftLines,
            issueDate = s.draftIssueDate,
            dueDate = s.draftDueDate,
            notes = s.draftNotes,
            terms = s.draftTerms,
            template = s.draftTemplate,
            taxRate = s.draftTaxRate.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            status = status,
        )
        clearDraft(); reload(); msg("Invoice saved"); onDone(id)
    }

    // ---- invoice actions ----
    fun markPaid(id: Long) = viewModelScope.launch { repo.markPaid(id); reload(); msg("Marked paid") }
    fun deleteInvoice(id: Long) = viewModelScope.launch { repo.deleteInvoice(id); reload(); msg("Invoice deleted") }

    fun duplicateInvoice(id: Long) = viewModelScope.launch {
        val inv = repo.invoice(id) ?: return@launch
        val lines = inv.lines.map {
            LineItemInput(
                description = it.description,
                quantity = runCatching { BigDecimal(it.quantity) }.getOrElse { BigDecimal.ONE },
                unitPrice = Money.of(it.unitPriceMinor),
                discount = Money.of(it.discountMinor),
            )
        }.ifEmpty { listOf(LineItemInput()) }
        _state.update {
            it.copy(
                draftCustomerId = null,
                draftOneOff = CustomerEntity(name = inv.invoice.customerName, contactPerson = inv.invoice.customerContact,
                    address = inv.invoice.customerAddress, email = inv.invoice.customerEmail, phone = inv.invoice.customerPhone),
                draftLines = lines, draftIssueDate = todayEpochDay(), draftDueDate = null,
                draftNotes = inv.invoice.notes, draftTerms = inv.invoice.terms, draftTemplate = inv.invoice.template,
                draftTaxRate = inv.invoice.taxRate,
            )
        }
        msg("Loaded as new draft")
    }

    // ---- PDF ----
    fun invoicePdf(id: Long, onFile: (File) -> Unit) = viewModelScope.launch {
        val inv = repo.invoice(id) ?: return@launch
        val profile = repo.businessProfile()
        onFile(InvoicePdf.writePdf(getApplication(), inv, profile, inv.invoice.template))
    }

    suspend fun invoiceById(id: Long): InvoiceWithLines? = repo.invoice(id)

    fun msg(m: String) = _state.update { it.copy(message = m) }
    fun consumeMessage() = _state.update { it.copy(message = null) }
}
