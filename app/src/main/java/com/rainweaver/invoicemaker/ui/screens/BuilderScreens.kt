package com.rainweaver.invoicemaker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.invoicemaker.data.local.CatalogItemEntity
import com.rainweaver.invoicemaker.data.local.CustomerEntity
import com.rainweaver.invoicemaker.data.local.InvoiceWithLines
import com.rainweaver.invoicemaker.domain.InvoiceStatus
import com.rainweaver.invoicemaker.domain.LineItemInput
import com.rainweaver.invoicemaker.domain.Money
import com.rainweaver.invoicemaker.ui.*
import java.math.BigDecimal

private fun parseMoney(text: String): Money? = runCatching { Money.fromDecimal(text.trim()) }.getOrNull()
private fun parseDec(text: String): BigDecimal? = runCatching { BigDecimal(text.trim()) }.getOrNull()

// ---------- Invoice Builder ----------
@Composable
fun BuilderScreen(vm: InvoiceViewModel, onIssued: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var oneOff by remember { mutableStateOf(false) }
    var custMenu by remember { mutableStateOf(false) }
    var dueDays by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        // Customer
        item {
            SectionCard("Customer") {
                Box {
                    OutlinedButton(onClick = { custMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        val label = if (oneOff) "One-off customer" else s.draftCustomer?.name ?: "Select a customer"
                        Text(label)
                    }
                    DropdownMenu(expanded = custMenu, onDismissRequest = { custMenu = false }) {
                        s.customers.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { vm.setDraftCustomer(c.id); oneOff = false; custMenu = false })
                        }
                        DropdownMenuItem(text = { Text("One-off customer") }, onClick = { oneOff = true; vm.setDraftCustomer(null); custMenu = false })
                    }
                }
                if (oneOff || (s.draftCustomerId == null && s.draftOneOff.name.isNotBlank())) {
                    Spacer(Modifier.height(8.dp))
                    LabeledField("Customer name", s.draftOneOff.name) { vm.setDraftOneOff(s.draftOneOff.copy(name = it)) }
                    Spacer(Modifier.height(8.dp))
                    LabeledField("Address", s.draftOneOff.address) { vm.setDraftOneOff(s.draftOneOff.copy(address = it)) }
                    Spacer(Modifier.height(8.dp))
                    LabeledField("Email", s.draftOneOff.email) { vm.setDraftOneOff(s.draftOneOff.copy(email = it)) }
                }
            }
        }

        // Quick add from catalog
        if (s.items.isNotEmpty()) {
            item {
                SectionCard("Quick add from items") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.items) { ci ->
                            AssistChip(onClick = {
                                vm.addCatalogLine(
                                    ci.description,
                                    ci.unitPriceMinor,
                                    ci.defaultTaxPercent?.let { p -> runCatching { BigDecimal(p) }.getOrNull() },
                                )
                            }, label = { Text(ci.description) })
                        }
                    }
                }
            }
        }

        // Line items
        item { SectionCard("Line Items") {} }
        itemsIndexed(items = s.draftLines, key = { _, line -> line.id }) { i, line ->
            LineEditor(i, line, s.profile.currency, onChange = { vm.setDraftLine(line.id, it) }, onRemove = { vm.removeDraftLine(line.id) })
        }
        item {
            TextButton(onClick = { vm.addDraftLine() }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("+ Add line") }
        }

        // Dates + template
        item {
            SectionCard("Details") {
                Text("Issue date: ${fmtDate(s.draftIssueDate)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LabeledField("Due in (days, optional)", dueDays, keyboardType = KeyboardType.Number) {
                    dueDays = it
                    val n = it.trim().toIntOrNull()
                    vm.setDraftDates(s.draftIssueDate, if (n != null && n > 0) s.draftIssueDate + n else null)
                }
                Spacer(Modifier.height(12.dp))
                Text("Template", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MINIMAL", "CLASSIC", "MODERN").forEach { t ->
                        FilterChip(selected = s.draftTemplate == t, onClick = { vm.setDraftTemplate(t) }, label = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        }

        // Notes / terms
        item {
            SectionCard("Notes & Terms") {
                LabeledField("Notes (optional)", s.draftNotes, singleLine = false) { vm.setDraftNotes(it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Terms (optional)", s.draftTerms, singleLine = false) { vm.setDraftTerms(it) }
            }
        }

        // Totals + actions
        item {
            SectionCard("Total") {
                val t = s.draftTotals
                TotalRow("Subtotal", Money.of(t.subtotal.minor).format(s.profile.currency))
                if (t.totalDiscount.minor != 0L) TotalRow("Discount", "-" + Money.of(t.totalDiscount.minor).format(s.profile.currency))
                if (t.totalTax.minor != 0L) TotalRow("Tax", Money.of(t.totalTax.minor).format(s.profile.currency))
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                TotalRow("Grand Total", Money.of(t.grandTotal.minor).format(s.profile.currency), bold = true)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.saveInvoice(InvoiceStatus.DRAFT) }, modifier = Modifier.weight(1f)) { Text("Save Draft") }
                    Button(onClick = { vm.saveInvoice(InvoiceStatus.ISSUED, onDone = { onIssued() }) }, modifier = Modifier.weight(1f)) { Text("Issue") }
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun LineEditor(index: Int, line: LineItemInput, currency: String, onChange: (LineItemInput) -> Unit, onRemove: () -> Unit) {
    var desc by remember(line.id) { mutableStateOf(line.description) }
    var qty by remember(line.id) { mutableStateOf(if (line.quantity.stripTrailingZeros().toPlainString() == "1") "" else line.quantity.toPlainString()) }
    var price by remember(line.id) { mutableStateOf(if (line.unitPrice.minor == 0L) "" else Money.of(line.unitPrice.minor).format(currency).replace(Regex("[^0-9.]"), "")) }
    var discount by remember(line.id) { mutableStateOf(if (line.discount.minor == 0L) "" else Money.of(line.discount.minor).format(currency).replace(Regex("[^0-9.]"), "")) }
    var tax by remember(line.id) { mutableStateOf(line.taxPercent?.toPlainString() ?: "") }

    fun emit() = onChange(line.copy(
        description = desc,
        quantity = parseDec(qty.ifBlank { "1" }) ?: BigDecimal.ONE,
        unitPrice = parseMoney(price.ifBlank { "0" }) ?: Money.ZERO,
        discount = parseMoney(discount.ifBlank { "0" }) ?: Money.ZERO,
        taxPercent = parseDec(tax),
    ))

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Line ${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            }
            LabeledField("Description", desc) { desc = it; emit() }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Qty", qty, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { qty = it; emit() }
                LabeledField("Unit price", price, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1.4f)) { price = it; emit() }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Discount", discount, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1.4f)) { discount = it; emit() }
                LabeledField("Tax %", tax, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { tax = it; emit() }
            }
        }
    }
}

// ---------- History ----------
@Composable
fun HistoryScreen(vm: InvoiceViewModel, onOpen: (Long) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = s.invoices.filter {
        query.isBlank() || it.invoice.number.contains(query, true) || it.invoice.customerName.contains(query, true)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search number or customer") },
                modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true)
        }
        if (filtered.isEmpty()) item { EmptyState(if (s.invoices.isEmpty()) "No invoices yet." else "No matches.") }
        items(filtered, key = { it.invoice.id }) { inv ->
            ListItem(
                headlineContent = { Text("${inv.invoice.number}  •  ${inv.invoice.customerName}") },
                supportingContent = { Text("${fmtDate(inv.invoice.issueDate)}  •  ${Money.of(inv.invoice.grandTotalMinor).format(inv.invoice.currency)}") },
                leadingContent = { StatusChip(inv.invoice.status) },
                modifier = Modifier.clickable { onOpen(inv.invoice.id) },
            )
            HorizontalDivider()
        }
    }
}

// ---------- Detail ----------
@Composable
fun DetailScreen(vm: InvoiceViewModel, id: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    var inv by remember { mutableStateOf<InvoiceWithLines?>(null) }
    LaunchedEffect(id) { inv = vm.invoiceById(id) }
    val s by vm.state.collectAsStateWithLifecycle()
    val current = inv ?: s.invoices.firstOrNull { it.invoice.id == id }

    if (current == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item {
                SectionCard("${current.invoice.number}") {
                    Text(current.invoice.customerName, style = MaterialTheme.typography.titleMedium)
                    Text("Issued ${fmtDate(current.invoice.issueDate)}", style = MaterialTheme.typography.bodySmall)
                    current.invoice.dueDate?.let { Text("Due ${fmtDate(it)}", style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(4.dp)); StatusChip(current.invoice.status)
                }
            }
            item {
                SectionCard("Items") {
                    current.lines.forEach { l ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l.description, Modifier.weight(1f))
                            Text(Money.of(l.lineTotalMinor).format(current.invoice.currency))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    TotalRow("Subtotal", Money.of(current.invoice.subtotalMinor).format(current.invoice.currency))
                    if (current.invoice.totalDiscountMinor != 0L) TotalRow("Discount", "-" + Money.of(current.invoice.totalDiscountMinor).format(current.invoice.currency))
                    if (current.invoice.totalTaxMinor != 0L) TotalRow("Tax", Money.of(current.invoice.totalTaxMinor).format(current.invoice.currency))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    TotalRow("Total", Money.of(current.invoice.grandTotalMinor).format(current.invoice.currency), bold = true)
                }
            }
            if (current.invoice.notes.isNotBlank() || current.invoice.terms.isNotBlank() || current.invoice.businessPaymentInstructions.isNotBlank()) {
                item {
                    SectionCard("Notes") {
                        if (current.invoice.businessPaymentInstructions.isNotBlank()) Text("Payment: ${current.invoice.businessPaymentInstructions}")
                        if (current.invoice.notes.isNotBlank()) Text("Notes: ${current.invoice.notes}")
                        if (current.invoice.terms.isNotBlank()) Text("Terms: ${current.invoice.terms}")
                    }
                }
            }
        }
        // Action bar
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.invoicePdf(id) { sharePdf(context, it) } }, modifier = Modifier.weight(1f)) { Text("Share PDF") }
                if (current.invoice.status != "PAID") OutlinedButton(onClick = { vm.markPaid(id) }, modifier = Modifier.weight(1f)) { Text("Mark Paid") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.duplicateInvoice(id); onBack() }) { Text("Duplicate") }
            TextButton(onClick = { vm.deleteInvoice(id); onBack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
