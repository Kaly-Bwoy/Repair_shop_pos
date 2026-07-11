package com.mobilehub.pos.data.local.dao

import androidx.room.*
import com.mobilehub.pos.data.local.entity.Expense
import com.mobilehub.pos.data.local.entity.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY dateTime DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY dateTime DESC")
    fun getAllExpensesSync(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpenseSync(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp")
    fun getTotalExpensesBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE dateTime >= :startTimestamp AND dateTime <= :endTimestamp")
    suspend fun getTotalExpensesBetweenSync(startTimestamp: Long, endTimestamp: Long): Double?

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: Settings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSettingSync(setting: Settings)

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<Settings>>

    @Query("SELECT * FROM settings")
    fun getAllSettingsSync(): List<Settings>
}
