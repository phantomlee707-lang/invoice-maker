package com.rainweaver.invoicemaker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.invoicemaker.data.local.BusinessProfileEntity
import com.rainweaver.invoicemaker.data.local.CatalogItemEntity
import com.rainweaver.invoicemaker.data.local.CustomerEntity
import com.rainweaver.invoicemaker.data.local.InvoiceWithLines
import com.rainweaver.invoicemaker.domain.InvoiceStatus
import com.rainweaver.invoicemaker.domain.LineItemInput
import com.rainweaver.invoicemaker.domain.Money
import java.math.BigDecimal

// ---------- helpers ----------
private fun parseMoney(text: String): Money? =
    runCatching { Money.fromDecimal(text.trim()) }.getOrNull()

private fun parseDec(text: String): BigDecimal? =
    runCatching { BigDecimal(text.trim()) }.getOrNull()

// ---------- Business Profile ----------
@Composable
fun ProfileScreen(vm: InvoiceViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var p by remember(s.profile) { mutableStateOf(s.profile) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { vm.msg(if (Backup.export(context, it)) "Backup saved" else "Backup failed") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { if (Backup.restore(context, it)) { vm.reload(); vm.msg("Restored — reopen app if data looks stale") } else vm.msg("Restore failed") }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            SectionCard("Business Profile") {
                LabeledField("Business / trading name", p.name) { p = p.copy(name = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Contact name (optional)", p.contactName) { p = p.copy(contactName = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Address (optional)", p.address) { p = p.copy(address = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Phone (optional)", p.phone) { p = p.copy(phone = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Email (optional)", p.email) { p = p.copy(email = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Currency code", p.currency) { p = p.copy(currency = it.trim().uppercase().take(6)) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Payment instructions (optional)", p.paymentInstructions, singleLine = false) { p = p.copy(paymentInstructions = it) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.saveProfile(p) }, modifier = Modifier.fillMaxWidth()) { Text("Save Profile") }
            }
        }
        item {
            SectionCard("Backup & Restore") {
                Text("Export or restore all your data locally. Backups are never uploaded.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("invoice-maker-backup.db") }, modifier = Modifier.weight(1f)) { Text("Backup") }
                    Button(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }
        }
    }
}

// ---------- Customers ----------
@Composable
fun CustomersScreen(vm: InvoiceViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Button(onClick = { editing = CustomerEntity(name = ""); showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("+ Add Customer") }
        }
        if (s.customers.isEmpty()) item { EmptyState("No customers yet. Tap + to add one.") }
        items(s.customers, key = { it.id }) { c ->
            ListItem(
                headlineContent = { Text(c.name) },
                supportingContent = { Text(listOf(c.email, c.phone).filter { it.isNotBlank() }.joinToString("  •  ")) },
                trailingContent = {
                    Row {
                        TextButton(onClick = { editing = c; showDialog = true }) { Text("Edit") }
                        TextButton(onClick = { vm.deleteCustomer(c.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
            HorizontalDivider()
        }
    }
    if (showDialog && editing != null) CustomerDialog(editing!!, onDismiss = { showDialog = false }, onSave = { vm.saveCustomer(it); showDialog = false })
}

@Composable
private fun CustomerDialog(c: CustomerEntity, onDismiss: () -> Unit, onSave: (CustomerEntity) -> Unit) {
    var e by remember { mutableStateOf(c) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (c.id == 0L) "New Customer" else "Edit Customer") },
        text = {
            Column {
                LabeledField("Name *", e.name) { e = e.copy(name = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Contact person", e.contactPerson) { e = e.copy(contactPerson = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Address", e.address) { e = e.copy(address = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Email", e.email) { e = e.copy(email = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Phone", e.phone) { e = e.copy(phone = it) }
            }
        },
        confirmButton = { Button(onClick = { if (e.name.isNotBlank()) onSave(e) }, enabled = e.name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------- Items / Catalog ----------
@Composable
fun ItemsScreen(vm: InvoiceViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Button(onClick = { editing = CatalogItemEntity(description = "", unitPriceMinor = 0); showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("+ Add Item / Service") }
        }
        if (s.items.isEmpty()) item { EmptyState("No items yet. Tap + to add one.") }
        items(s.items, key = { it.id }) { it ->
            ListItem(
                headlineContent = { Text(it.description) },
                supportingContent = { Text(Money.of(it.unitPriceMinor).format(s.profile.currency) + (if (it.unit.isNotBlank()) " / ${it.unit}" else "")) },
                trailingContent = {
                    Row {
                        TextButton(onClick = { editing = it; showDialog = true }) { Text("Edit") }
                        TextButton(onClick = { vm.deleteItem(it.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
            HorizontalDivider()
        }
    }
    if (showDialog && editing != null) ItemDialog(editing!!, s.profile.currency, onDismiss = { showDialog = false }, onSave = { vm.saveItem(it); showDialog = false })
}

@Composable
private fun ItemDialog(it0: CatalogItemEntity, currency: String, onDismiss: () -> Unit, onSave: (CatalogItemEntity) -> Unit) {
    var e by remember { mutableStateOf(it0) }
    var price by remember { mutableStateOf(if (it0.unitPriceMinor == 0L) "" else Money.of(it0.unitPriceMinor).format(currency).replace(Regex("[^0-9.]"), "")) }
    var tax by remember { mutableStateOf(it0.defaultTaxPercent ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (it0.id == 0L) "New Item" else "Edit Item") },
        text = {
            Column {
                LabeledField("Description *", e.description) { e = e.copy(description = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Unit price", price, keyboardType = KeyboardType.Decimal) { price = it; parseMoney(it)?.let { m -> e = e.copy(unitPriceMinor = m.minor) } }
                Spacer(Modifier.height(8.dp))
                LabeledField("Unit (e.g. hr, each)", e.unit) { e = e.copy(unit = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Default tax % (optional)", tax, keyboardType = KeyboardType.Decimal) { tax = it; e = e.copy(defaultTaxPercent = it.ifBlank { null }) }
            }
        },
        confirmButton = { Button(onClick = { if (e.description.isNotBlank()) onSave(e) }, enabled = e.description.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
