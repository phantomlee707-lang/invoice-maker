package com.rainweaver.invoicemaker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.invoicemaker.R
import com.rainweaver.invoicemaker.ui.screens.*

private enum class Tab(val label: String, @DrawableRes val iconRes: Int) {
    NEW("New", R.drawable.ic_tab_new),
    HISTORY("History", R.drawable.ic_tab_history),
    CUSTOMERS("Customers", R.drawable.ic_tab_customers),
    ITEMS("Items", R.drawable.ic_tab_items),
    PROFILE("Profile", R.drawable.ic_tab_profile),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(vm: InvoiceViewModel) {
    var tab by remember { mutableStateOf(Tab.NEW) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Invoice Maker", fontWeight = FontWeight.SemiBold)
                    Text("Rainweaver Studio", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && detailId == null,
                        onClick = { tab = t; detailId = null },
                        icon = { Icon(painter = painterResource(t.iconRes), contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val id = detailId
            if (id != null) {
                DetailScreen(vm, id, onBack = { detailId = null })
            } else when (tab) {
                Tab.NEW -> BuilderScreen(vm, onIssued = { tab = Tab.HISTORY })
                Tab.HISTORY -> HistoryScreen(vm, onOpen = { detailId = it })
                Tab.CUSTOMERS -> CustomersScreen(vm)
                Tab.ITEMS -> ItemsScreen(vm)
                Tab.PROFILE -> ProfileScreen(vm)
            }
        }
    }
}
