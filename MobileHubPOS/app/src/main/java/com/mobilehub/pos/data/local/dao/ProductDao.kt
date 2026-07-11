package com.mobilehub.pos.data.local.dao

import androidx.room.*
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.Category
import com.mobilehub.pos.data.local.entity.StockHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsSync(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE sku = :sku")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE stockQuantity <= lowStockThreshold")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProductSync(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :change WHERE id = :productId")
    suspend fun adjustStock(productId: Long, change: Int)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesSync(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategorySync(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    // --- Stock History ---
    @Query("SELECT * FROM stock_history WHERE productId = :productId ORDER BY dateTime DESC")
    fun getStockHistoryByProduct(productId: Long): Flow<List<StockHistory>>

    @Query("SELECT * FROM stock_history ORDER BY dateTime DESC")
    fun getAllStockHistorySync(): List<StockHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockHistory(history: StockHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStockHistorySync(history: StockHistory): Long

    @Query("SELECT * FROM stock_history ORDER BY dateTime DESC LIMIT 100")
    fun getRecentStockMovement(): Flow<List<StockHistory>>
}
