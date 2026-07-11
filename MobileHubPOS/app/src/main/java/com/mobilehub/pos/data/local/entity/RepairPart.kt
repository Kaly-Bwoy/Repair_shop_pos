package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repair_parts")
data class RepairPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repairId: Long,
    val productId: Long?, // Null if a generic part not in our inventory
    val partName: String,
    val quantity: Int,
    val costPrice: Double, // Cost price to the shop
    val sellingPrice: Double, // Selling price charged to customer
    val isAutoDeducted: Boolean = false // Track if we deducted from product stock
)
