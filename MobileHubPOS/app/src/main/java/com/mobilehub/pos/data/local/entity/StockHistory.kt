package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_history")
data class StockHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val changeType: String, // "Restock", "Sale", "Repair", "Adjustment", "Return"
    val quantityChanged: Int, // Can be positive (restock) or negative (sale/repair)
    val dateTime: Long = System.currentTimeMillis(),
    val notes: String? = null
)
