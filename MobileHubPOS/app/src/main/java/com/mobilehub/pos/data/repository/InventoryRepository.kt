package com.mobilehub.pos.data.repository

import com.mobilehub.pos.data.local.dao.ProductDao
import com.mobilehub.pos.data.local.entity.Category
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.StockHistory
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCategories: Flow<List<Category>> = productDao.getAllCategories()
    val recentStockMovement: Flow<List<StockHistory>> = productDao.getRecentStockMovement()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    suspend fun getProductBySku(sku: String): Product? = productDao.getProductBySku(sku)

    suspend fun insertProduct(product: Product): Long {
        val id = productDao.insertProduct(product)
        // Record initial stock if greater than zero
        if (product.stockQuantity > 0) {
            productDao.insertStockHistory(
                StockHistory(
                    productId = id,
                    changeType = "Adjustment",
                    quantityChanged = product.stockQuantity,
                    notes = "Initial stock setup"
                )
            )
        }
        return id
    }

    suspend fun updateProduct(product: Product, reason: String? = "Manual Update") {
        val oldProduct = productDao.getProductById(product.id)
        productDao.updateProduct(product)
        if (oldProduct != null && oldProduct.stockQuantity != product.stockQuantity) {
            val diff = product.stockQuantity - oldProduct.stockQuantity
            productDao.insertStockHistory(
                StockHistory(
                    productId = product.id,
                    changeType = "Adjustment",
                    quantityChanged = diff,
                    notes = reason
                )
            )
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun addStock(productId: Long, quantity: Int, notes: String? = "Restock") {
        productDao.adjustStock(productId, quantity)
        productDao.insertStockHistory(
            StockHistory(
                productId = productId,
                changeType = "Restock",
                quantityChanged = quantity,
                notes = notes
            )
        )
    }

    suspend fun insertCategory(category: Category): Long = productDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = productDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = productDao.deleteCategory(category)
    suspend fun getCategoryById(id: Long): Category? = productDao.getCategoryById(id)
    
    fun getStockHistoryForProduct(productId: Long): Flow<List<StockHistory>> =
        productDao.getStockHistoryByProduct(productId)
}
