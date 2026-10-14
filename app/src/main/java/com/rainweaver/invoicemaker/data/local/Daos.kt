package com.rainweaver.invoicemaker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun get(): BusinessProfileEntity?

    @Insert
    suspend fun insert(profile: BusinessProfileEntity)

    @Update
    suspend fun update(profile: BusinessProfileEntity): Int

    @Transaction
    suspend fun upsert(profile: BusinessProfileEntity) {
        if (update(profile) == 0) insert(profile)
    }
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE")
    suspend fun search(q: String): List<CustomerEntity>

    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CatalogItemDao {
    @Query("SELECT * FROM catalog_items ORDER BY description COLLATE NOCASE")
    suspend fun getAll(): List<CatalogItemEntity>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getById(id: Long): CatalogItemEntity?

    @Query("SELECT * FROM catalog_items WHERE description LIKE '%' || :q || '%' ORDER BY description COLLATE NOCASE")
    suspend fun search(q: String): List<CatalogItemEntity>

    @Insert
    suspend fun insert(item: CatalogItemEntity): Long

    @Update
    suspend fun update(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert
    suspend fun insert(settings: AppSettingsEntity)

    @Update
    suspend fun update(settings: AppSettingsEntity): Int

    @Transaction
    suspend fun upsert(settings: AppSettingsEntity) {
        if (update(settings) == 0) insert(settings)
    }
}

/** Invoice + lines must persist atomically, so this DAO is an abstract class. */
@Dao
abstract class InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    abstract suspend fun getAllWithLines(): List<InvoiceWithLines>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    abstract suspend fun getByIdWithLines(id: Long): InvoiceWithLines?

    @Query("SELECT * FROM invoices WHERE number = :number LIMIT 1")
    abstract suspend fun findByNumber(number: String): InvoiceEntity?

    @Insert
    abstract suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert
    abstract suspend fun insertLines(lines: List<InvoiceLineEntity>)

    @Update
    abstract suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    abstract suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM invoices WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Transaction
    open suspend fun insertInvoiceWithLines(invoice: InvoiceEntity, lines: List<InvoiceLineEntity>): Long {
        val id = insertInvoice(invoice)
        insertLines(lines.map { it.copy(invoiceId = id) })
        return id
    }
}

/** An invoice header joined to its frozen line snapshots. */
data class InvoiceWithLines(
    @androidx.room.Embedded val invoice: InvoiceEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "invoiceId")
    val lines: List<InvoiceLineEntity>,
)
