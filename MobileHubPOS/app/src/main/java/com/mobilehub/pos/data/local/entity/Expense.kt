package com.mobilehub.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String, // e.g. "Rent", "Electricity", "Tools", "Salaries"
    val description: String? = null,
    val dateTime: Long = System.currentTimeMillis()
)
