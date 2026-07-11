package com.mobilehub.pos.data.local.dao

import androidx.room.*
import com.mobilehub.pos.data.local.entity.Sale
import com.mobilehub.pos.data.local.entity.SaleItem
import com.mobilehub.pos.data.local.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSaleSync(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSaleItemSync(saleItem: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPaymentSync(payment: Payment): Long

    @Transaction
    @Query("SELECT * FROM sales ORDER BY dateTime DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY dateTime DESC")
    fun getAllSalesSync(): List<Sale>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItemsSync(): List<SaleItem>

    @Query("SELECT * FROM payments")
    fun getAllPaymentsSync(): List<Payment>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsBySaleId(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsBySaleIdSync(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY dateTime DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    fun getPaymentsBySaleId(saleId: Long): Flow<List<Payment>>

    // For Reports and Dashboard
    @Query("SELECT SUM(totalAmount) FROM sales WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp")
    fun getTotalRevenueBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp")
    suspend fun getTotalRevenueBetweenSync(startTimestamp: Long, endTimestamp: Long): Double?

    @Query("SELECT * FROM sales WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp ORDER BY dateTime DESC")
    fun getSalesBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp ORDER BY dateTime DESC")
    suspend fun getSalesBetweenSync(startTimestamp: Long, endTimestamp: Long): List<Sale>
}
