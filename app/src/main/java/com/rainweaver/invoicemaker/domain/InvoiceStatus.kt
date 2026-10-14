package com.rainweaver.invoicemaker.domain

/** Invoice lifecycle. OVERDUE is derived (ISSUED with a past due date), never stored. */
enum class InvoiceStatus { DRAFT, ISSUED, PAID }
