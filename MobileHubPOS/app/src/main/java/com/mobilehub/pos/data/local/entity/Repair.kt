package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val deviceType: String, // "Phone", "Tablet", "Laptop", "Other"
    val brand: String,
    val model: String,
    val imeiSerial: String? = null,
    val passwordPattern: String? = null,
    val conditionNotes: String? = null,
    val faultDescription: String,
    val status: String, // "Received", "Diagnosing", "Waiting for parts", "Repairing", "Completed", "Collected"
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0, // calculated from parts used
    val totalEstimatedCost: Double = 0.0,
    val finalCost: Double = 0.0,
    val amountPaid: Double = 0.0,
    val notes: String? = null,
    val imagePath1: String? = null, // local photo path
    val imagePath2: String? = null, // local photo path
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
