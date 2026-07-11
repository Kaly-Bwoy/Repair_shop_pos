package com.mobilehub.pos.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.entity.Category
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.StockHistory
import com.mobilehub.pos.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryState(
    val productsList: List<Product> = emptyList(),
    val categoriesList: List<Category> = emptyList(),
    val stockHistoryList: List<StockHistory> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategoryFilter: Long? = null,
    val searchQuery: String = "",
    val activeTab: Int = 0 // 0: Products, 1: Categories, 2: Stock History
)

class InventoryViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            combine(
                inventoryRepository.allProducts,
                inventoryRepository.allCategories,
                inventoryRepository.recentStockMovement
            ) { prods, cats, history ->
                InventoryState(
                    productsList = prods,
                    categoriesList = cats,
                    stockHistoryList = history,
                    isLoading = false,
                    selectedCategoryFilter = _state.value.selectedCategoryFilter,
                    searchQuery = _state.value.searchQuery,
                    activeTab = _state.value.activeTab
                )
            }.collect { updatedState ->
                _state.value = updatedState
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(categoryId: Long?) {
        _state.update { it.copy(selectedCategoryFilter = categoryId) }
    }

    fun setActiveTab(tabIndex: Int) {
        _state.update { it.copy(activeTab = tabIndex) }
    }

    fun addProduct(name: String, sku: String, purchaseCost: Double, sellingPrice: Double, quantity: Int, lowThreshold: Int, categoryId: Long?, isPart: Boolean, imageUri: String? = null) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                sku = sku,
                purchaseCost = purchaseCost,
                sellingPrice = sellingPrice,
                stockQuantity = quantity,
                lowStockThreshold = lowThreshold,
                categoryId = categoryId,
                isPart = isPart,
                imageUri = imageUri
            )
            inventoryRepository.insertProduct(product)
        }
    }

    fun updateProduct(product: Product, quantityChangedReason: String?) {
        viewModelScope.launch {
            inventoryRepository.updateProduct(product, quantityChangedReason)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            inventoryRepository.deleteProduct(product)
        }
    }

    fun addCategory(name: String, description: String?) {
        viewModelScope.launch {
            inventoryRepository.insertCategory(Category(name = name, description = description))
        }
    }

    fun restockProduct(productId: Long, quantityToAdd: Int, notes: String) {
        viewModelScope.launch {
            inventoryRepository.addStock(productId, quantityToAdd, notes)
        }
    }
}
