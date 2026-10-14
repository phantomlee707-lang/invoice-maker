package com.rainweaver.invoicemaker.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** epochDay (Long) -> "MMM d, yyyy". Avoids java.time so min SDK 24 needs no desugaring. */
fun fmtDate(epochDay: Long, locale: Locale = Locale.US): String =
    SimpleDateFormat("MMM d, yyyy", locale).format(Date(epochDay * 86_400_000L))

/** Today as epochDay (UTC). */
fun todayEpochDay(): Long = System.currentTimeMillis() / 86_400_000L
