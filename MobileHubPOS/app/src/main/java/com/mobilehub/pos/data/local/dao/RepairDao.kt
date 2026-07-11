package com.mobilehub.pos.data.local.dao

import androidx.room.*
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.RepairPart
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepair(repair: Repair): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRepairSync(repair: Repair): Long

    @Update
    suspend fun updateRepair(repair: Repair)

    @Delete
    suspend fun deleteRepair(repair: Repair)

    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAllRepairs(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAllRepairsSync(): List<Repair>

    @Query("SELECT * FROM repairs WHERE id = :id")
    suspend fun getRepairById(id: Long): Repair?

    @Query("SELECT * FROM repairs WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getRepairsByCustomer(customerId: Long): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE status = :status ORDER BY createdAt DESC")
    fun getRepairsByStatus(status: String): Flow<List<Repair>>

    // Repair Parts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepairPart(repairPart: RepairPart): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRepairPartSync(repairPart: RepairPart): Long

    @Delete
    suspend fun deleteRepairPart(repairPart: RepairPart)

    @Query("SELECT * FROM repair_parts WHERE repairId = :repairId")
    fun getRepairPartsForRepair(repairId: Long): Flow<List<RepairPart>>

    @Query("SELECT * FROM repair_parts WHERE repairId = :repairId")
    suspend fun getRepairPartsForRepairSync(repairId: Long): List<RepairPart>

    @Query("SELECT * FROM repair_parts")
    fun getAllRepairPartsSync(): List<RepairPart>

    @Query("SELECT SUM(finalCost) FROM repairs WHERE updatedAt >= :startTimestamp AND updatedAt <= :endTimestamp AND status = 'Collected'")
    fun getRepairIncomeBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(finalCost) FROM repairs WHERE updatedAt >= :startTimestamp AND updatedAt <= :endTimestamp AND status = 'Collected'")
    suspend fun getRepairIncomeBetweenSync(startTimestamp: Long, endTimestamp: Long): Double?

    @Query("SELECT * FROM repairs WHERE updatedAt >= :startTimestamp AND updatedAt <= :endTimestamp AND status = 'Collected'")
    suspend fun getRepairsBetweenSync(startTimestamp: Long, endTimestamp: Long): List<Repair>
}
