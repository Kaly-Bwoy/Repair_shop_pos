package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?, // Null if walk-in
    val dateTime: Long, // Epoch timestamp in milliseconds
    val subtotal: Double,
    val discount: Double, // total discount applied
    val tax: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String, // "Cash", "Mobile Money", "Card", etc.
    val notes: String? = null
)
