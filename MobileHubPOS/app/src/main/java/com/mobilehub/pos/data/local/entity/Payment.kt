package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long? = null, // Linked to a sale, if any
    val repairId: Long? = null, // Linked to a repair, if any
    val amount: Double,
    val paymentMethod: String, // "Cash", "Mobile Money", "Card", etc.
    val dateTime: Long = System.currentTimeMillis(),
    val notes: String? = null
)
