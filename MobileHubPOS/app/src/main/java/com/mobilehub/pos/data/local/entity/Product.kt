package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val name: String,
    val sku: String, // Barcode/SKU
    val description: String? = null,
    val purchaseCost: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 5,
    val imageUri: String? = null, // Local path of product photo
    val isPart: Boolean = false // True if this item is a repair part (e.g., iPhone replacement screen)
)
