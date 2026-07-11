package com.mobilehub.pos.data.local.dao

import androidx.room.*
import com.mobilehub.pos.data.local.entity.StoreProfile
import com.mobilehub.pos.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    // --- Store Profile ---
    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    fun getStoreProfileFlow(): Flow<StoreProfile?>

    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    suspend fun getStoreProfile(): StoreProfile?

    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    fun getStoreProfileSync(): StoreProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreProfile(profile: StoreProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStoreProfileSync(profile: StoreProfile)

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsersSync(): List<User>

    @Query("SELECT * FROM users WHERE username = :username AND passwordPin = :pin LIMIT 1")
    suspend fun authenticateUser(username: String, pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserSync(user: User): Long

    @Delete
    suspend fun deleteUser(user: User)
}
