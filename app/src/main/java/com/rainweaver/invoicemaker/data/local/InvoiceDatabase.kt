package com.rainweaver.invoicemaker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BusinessProfileEntity::class,
        CustomerEntity::class,
        CatalogItemEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class InvoiceDatabase : RoomDatabase() {

    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun catalogItemDao(): CatalogItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun get(context: Context): InvoiceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_maker.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }

        /** Close and drop the singleton so a restored database is reopened fresh. */
        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
