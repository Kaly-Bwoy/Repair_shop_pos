package com.mobilehub.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobilehub.pos.data.local.dao.*
import com.mobilehub.pos.data.local.entity.*

@Database(
    entities = [
        Category::class,
        Product::class,
        Customer::class,
        Sale::class,
        SaleItem::class,
        Repair::class,
        RepairPart::class,
        Payment::class,
        Expense::class,
        StockHistory::class,
        Settings::class,
        StoreProfile::class,
        User::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun repairDao(): RepairDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun adminDao(): AdminDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilehub_pos_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
