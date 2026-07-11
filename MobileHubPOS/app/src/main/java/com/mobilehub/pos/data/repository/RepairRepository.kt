package com.mobilehub.pos.data.repository

import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.RepairPart
import com.mobilehub.pos.data.local.entity.StockHistory
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class RepairRepository(private val db: AppDatabase) {
    private val repairDao = db.repairDao()
    private val productDao = db.productDao()
    private val saleDao = db.saleDao()

    val allRepairs: Flow<List<Repair>> = repairDao.getAllRepairs()

    fun getRepairsByStatus(status: String): Flow<List<Repair>> = repairDao.getRepairsByStatus(status)
    fun getRepairPartsForRepair(repairId: Long): Flow<List<RepairPart>> = repairDao.getRepairPartsForRepair(repairId)
    suspend fun getRepairById(id: Long): Repair? = repairDao.getRepairById(id)

    suspend fun createRepair(repair: Repair): Long {
        return repairDao.insertRepair(repair)
    }

    suspend fun updateRepair(repair: Repair) {
        repairDao.updateRepair(repair.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRepair(repair: Repair) {
        repairDao.deleteRepair(repair)
    }

    // Add part to repair, auto deduct from inventory
    suspend fun addRepairPart(repairId: Long, productId: Long?, partName: String, quantity: Int, costPrice: Double, sellingPrice: Double): Long {
        return db.withTransaction {
            val part = RepairPart(
                repairId = repairId,
                productId = productId,
                partName = partName,
                quantity = quantity,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                isAutoDeducted = productId != null
            )
            val partId = repairDao.insertRepairPart(part)

            // Auto-deduct stock if linked to product
            if (productId != null) {
                val product = productDao.getProductById(productId)
                if (product != null) {
                    productDao.adjustStock(productId, -quantity)
                    productDao.insertStockHistory(
                        StockHistory(
                            productId = productId,
                            changeType = "Repair",
                            quantityChanged = -quantity,
                            notes = "Used in repair #$repairId"
                        )
                    )
                }
            }

            // Recalculate repair costs
            recalculateRepairCosts(repairId)

            partId
        }
    }

    // Remove part from repair and refund stock if auto-deducted
    suspend fun removeRepairPart(part: RepairPart) {
        db.withTransaction {
            repairDao.deleteRepairPart(part)

            // Re-add stock if it was auto-deducted
            if (part.isAutoDeducted && part.productId != null) {
                productDao.adjustStock(part.productId, part.quantity)
                productDao.insertStockHistory(
                    StockHistory(
                        productId = part.productId,
                        changeType = "Adjustment",
                        quantityChanged = part.quantity,
                        notes = "Returned from cancelled repair part for repair #${part.repairId}"
                    )
                )
            }

            // Recalculate repair costs
            recalculateRepairCosts(part.repairId)
        }
    }

    private suspend fun recalculateRepairCosts(repairId: Long) {
        val repair = repairDao.getRepairById(repairId) ?: return
        val parts = repairDao.getRepairPartsForRepairSync(repairId)
        val totalPartsCost = parts.sumOf { it.sellingPrice * it.quantity }
        val finalCost = repair.laborCost + totalPartsCost

        val updatedRepair = repair.copy(
            partsCost = totalPartsCost,
            totalEstimatedCost = repair.laborCost + totalPartsCost,
            finalCost = finalCost,
            updatedAt = System.currentTimeMillis()
        )
        repairDao.updateRepair(updatedRepair)
    }

    // Record custom payment for a repair
    suspend fun addRepairPayment(repairId: Long, amount: Double, paymentMethod: String, notes: String? = null) {
        db.withTransaction {
            val repair = repairDao.getRepairById(repairId) ?: return@withTransaction
            val newAmountPaid = repair.amountPaid + amount
            val updatedRepair = repair.copy(
                amountPaid = newAmountPaid,
                status = if (newAmountPaid >= repair.finalCost) "Collected" else repair.status,
                updatedAt = System.currentTimeMillis()
            )
            repairDao.updateRepair(updatedRepair)

            // Register payment
            val payment = com.mobilehub.pos.data.local.entity.Payment(
                saleId = null,
                repairId = repairId,
                amount = amount,
                paymentMethod = paymentMethod,
                dateTime = System.currentTimeMillis(),
                notes = notes ?: "Payment for repair #$repairId"
            )
            saleDao.insertPayment(payment)
        }
    }
}
