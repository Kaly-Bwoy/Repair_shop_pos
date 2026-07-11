package com.mobilehub.pos

import android.app.Application
import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MobileHubApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database & Repositories initialized lazily when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val inventoryRepository by lazy { InventoryRepository(database.productDao()) }
    val salesRepository by lazy { SalesRepository(database) }
    val customerRepository by lazy { CustomerRepository(database.customerDao(), database.saleDao(), database.repairDao()) }
    val repairRepository by lazy { RepairRepository(database) }
    val backupRepository by lazy { BackupRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // Initialize default settings if they don't exist
        initializeSettings()
    }

    private fun initializeSettings() {
        applicationScope.launch {
            val expenseDao = database.expenseDao()
            val defaults = mapOf(
                "shop_name" to "MobileHub POS",
                "shop_phone" to "+233 24 123 4567",
                "shop_address" to "Main Street, Accra, Ghana",
                "currency_symbol" to "GH₵",
                "tax_rate" to "0.0",
                "low_stock_alert_threshold" to "5"
            )
            defaults.forEach { (key, value) ->
                if (expenseDao.getSetting(key) == null) {
                    expenseDao.saveSetting(com.mobilehub.pos.data.local.entity.Settings(key, value))
                }
            }
        }
    }
}
