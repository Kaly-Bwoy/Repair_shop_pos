package com.mobilehub.pos.data.repository

import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.local.entity.Sale
import com.mobilehub.pos.data.local.entity.SaleItem
import com.mobilehub.pos.data.local.entity.Payment
import com.mobilehub.pos.data.local.entity.StockHistory
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class SalesRepository(private val db: AppDatabase) {
    private val saleDao = db.saleDao()
    private val productDao = db.productDao()

    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    fun getSaleItemsBySaleId(saleId: Long): Flow<List<SaleItem>> = saleDao.getSaleItemsBySaleId(saleId)
    fun getPaymentsBySaleId(saleId: Long): Flow<List<Payment>> = saleDao.getPaymentsBySaleId(saleId)
    fun getSalesByCustomer(customerId: Long): Flow<List<Sale>> = saleDao.getSalesByCustomer(customerId)

    suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)

    // Complete POS checkout
    suspend fun checkout(
        customerId: Long?,
        subtotal: Double,
        discount: Double,
        tax: Double,
        totalAmount: Double,
        paymentMethod: String,
        items: List<CartItem>,
        notes: String? = null
    ): Long {
        return db.withTransaction {
            // 1. Create Sale
            val sale = Sale(
                customerId = customerId,
                dateTime = System.currentTimeMillis(),
                subtotal = subtotal,
                discount = discount,
                tax = tax,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            val saleId = saleDao.insertSale(sale)

            // 2. Insert items, deduct stock, record stock history
            for (cartItem in items) {
                val saleItem = SaleItem(
                    saleId = saleId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.product.sellingPrice,
                    discount = cartItem.discount,
                    totalPrice = (cartItem.product.sellingPrice - cartItem.discount) * cartItem.quantity
                )
                saleDao.insertSaleItem(saleItem)

                // Deduct stock
                productDao.adjustStock(cartItem.product.id, -cartItem.quantity)

                // Record stock history
                productDao.insertStockHistory(
                    StockHistory(
                        productId = cartItem.product.id,
                        changeType = "Sale",
                        quantityChanged = -cartItem.quantity,
                        notes = "Sold in transaction #$saleId"
                    )
                )
            }

            // 3. Create payment
            val payment = Payment(
                saleId = saleId,
                repairId = null,
                amount = totalAmount,
                paymentMethod = paymentMethod,
                dateTime = System.currentTimeMillis(),
                notes = "POS Checkout payment"
            )
            saleDao.insertPayment(payment)

            saleId
        }
    }

    fun getTotalRevenueToday(): Flow<Double?> {
        val startOfToday = getStartOfToday()
        val endOfToday = getEndOfToday()
        return saleDao.getTotalRevenueBetween(startOfToday, endOfToday)
    }

    private fun getStartOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

// Data class representation of an item in the cart
data class CartItem(
    val product: com.mobilehub.pos.data.local.entity.Product,
    var quantity: Int,
    var discount: Double = 0.0 // Discount per item unit
)
