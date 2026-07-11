package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey val id: Long = 1, // Only single store profile supported locally
    val storeName: String,
    val logoPath: String? = null, // Local Uri or path to custom uploaded logo
    val phone: String,
    val address: String,
    val currency: String = "GH₵",
    val email: String? = null,
    val taxRate: Double = 0.0,
    val adminPin: String = "1234",
    val receiptFooterNotes: String? = "Thank you for your business!"
)
