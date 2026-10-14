package com.rainweaver.invoicemaker.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

/** BigDecimal <-> String so money/quantity/tax values persist exactly (no float). */
class Converters {
    @TypeConverter fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter fun stringToBigDecimal(value: String?): BigDecimal? =
        value?.let { runCatching { BigDecimal(it) }.getOrNull() }
}
