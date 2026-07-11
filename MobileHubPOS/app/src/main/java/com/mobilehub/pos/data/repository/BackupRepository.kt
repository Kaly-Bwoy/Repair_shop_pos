package com.mobilehub.pos.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.local.entity.*
import com.mobilehub.pos.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class BackupRepository(private val db: AppDatabase) {

    data class BackupData(
        val backupDate: Long,
        val categories: List<Category>,
        val products: List<Product>,
        val customers: List<Customer>,
        val sales: List<Sale>,
        val saleItems: List<SaleItem>,
        val repairs: List<Repair>,
        val repairParts: List<RepairPart>,
        val payments: List<Payment>,
        val expenses: List<Expense>,
        val stockHistory: List<StockHistory>,
        val settings: List<Settings>,
        val storeProfile: List<StoreProfile>,
        val users: List<User>
    )

    /**
     * Exports and encrypts all tables into a secure AES-128 ciphertext file using the Admin PIN as key.
     */
    suspend fun exportBackup(backupFile: File, key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val categories = db.productDao().getAllCategoriesSync()
            val products = db.productDao().getAllProductsSync()
            val customers = db.customerDao().getAllCustomersSync()
            val sales = db.saleDao().getAllSalesSync()
            val saleItems = db.saleDao().getAllSaleItemsSync()
            val repairs = db.repairDao().getAllRepairsSync()
            val repairParts = db.repairDao().getAllRepairPartsSync()
            val payments = db.saleDao().getAllPaymentsSync()
            val expenses = db.expenseDao().getAllExpensesSync()
            val stockHistory = db.productDao().getAllStockHistorySync()
            val settings = db.expenseDao().getAllSettingsSync()
            val storeProfile = listOfNotNull(db.adminDao().getStoreProfileSync())
            val users = db.adminDao().getAllUsersSync()

            val backupData = BackupData(
                backupDate = System.currentTimeMillis(),
                categories = categories,
                products = products,
                customers = customers,
                sales = sales,
                saleItems = saleItems,
                repairs = repairs,
                repairParts = repairParts,
                payments = payments,
                expenses = expenses,
                stockHistory = stockHistory,
                settings = settings,
                storeProfile = storeProfile,
                users = users
            )

            // Convert to clean JSON string
            val gson = GsonBuilder().create()
            val plainJson = gson.toJson(backupData)

            // Encrypt using our cryptographic AES function
            val encryptedCiphertext = SecurityUtils.encryptAES(plainJson, key)

            // Write ciphertext to the file
            FileWriter(backupFile).use { writer ->
                writer.write(encryptedCiphertext)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts and restores the database from a secure AES ciphertext file.
     */
    suspend fun importBackup(backupFile: File, key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read encrypted ciphertext from file
            val ciphertext = FileReader(backupFile).use { reader ->
                reader.readText()
            }

            // Decrypt JSON string
            val plainJson = SecurityUtils.decryptAES(ciphertext, key)

            // Parse back to structural classes
            val gson = Gson()
            val backupData = gson.fromJson(plainJson, BackupData::class.java)
                ?: return@withContext Result.failure(Exception("Decryption key error: Output string is not a valid database backup configuration file! Check your Admin PIN."))

            db.runInTransaction {
                db.clearAllTables()

                // Insert Categories
                backupData.categories.forEach {
                    db.productDao().insertCategorySync(it)
                }

                // Insert Products
                backupData.products.forEach {
                    db.productDao().insertProductSync(it)
                }

                // Insert Customers
                backupData.customers.forEach {
                    db.customerDao().insertCustomerSync(it)
                }

                // Insert Sales
                backupData.sales.forEach {
                    db.saleDao().insertSaleSync(it)
                }

                // Insert Sale Items
                backupData.saleItems.forEach {
                    db.saleDao().insertSaleItemSync(it)
                }

                // Insert Repairs
                backupData.repairs.forEach {
                    db.repairDao().insertRepairSync(it)
                }

                // Insert Repair Parts
                backupData.repairParts.forEach {
                    db.repairDao().insertRepairPartSync(it)
                }

                // Insert Payments
                backupData.payments.forEach {
                    db.saleDao().insertPaymentSync(it)
                }

                // Insert Expenses
                backupData.expenses.forEach {
                    db.expenseDao().insertExpenseSync(it)
                }

                // Insert Stock History
                backupData.stockHistory.forEach {
                    db.productDao().insertStockHistorySync(it)
                }

                // Insert Settings
                backupData.settings.forEach {
                    db.expenseDao().insertSettingSync(it)
                }

                // Insert Store Profile
                backupData.storeProfile.forEach {
                    db.adminDao().insertStoreProfileSync(it)
                }

                // Insert Users
                backupData.users.forEach {
                    db.adminDao().insertUserSync(it)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
