package com.rainweaver.invoicemaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainweaver.invoicemaker.ui.AppNav
import com.rainweaver.invoicemaker.ui.InvoiceViewModel
import com.rainweaver.invoicemaker.ui.theme.InvoiceMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InvoiceMakerTheme {
                val vm: InvoiceViewModel = viewModel()
                AppNav(vm)
            }
        }
    }
}
