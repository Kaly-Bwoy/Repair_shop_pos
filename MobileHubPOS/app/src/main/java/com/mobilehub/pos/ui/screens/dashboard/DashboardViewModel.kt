package com.mobilehub.pos.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.Sale
import com.mobilehub.pos.data.local.entity.StockHistory
import com.mobilehub.pos.data.repository.InventoryRepository
import com.mobilehub.pos.data.repository.RepairRepository
import com.mobilehub.pos.data.repository.SalesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val todaySalesCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val totalRepairsCount: Int = 0,
    val pendingRepairsCount: Int = 0,
    val lowStockItems: List<Product> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = false
)

data class ActivityItem(
    val title: String,
    val timestamp: Long,
    val type: String // "SALE", "REPAIR", "STOCK"
)

class DashboardViewModel(
    private val salesRepository: SalesRepository,
    private val repairRepository: RepairRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Combine flows for reactive dashboard updates
            // SECURE FIX: Added inventoryRepository.allProducts flow to perform fast synchronous lookups of product names
            // inside the combine block, avoiding illegal suspend-function calls inside a non-suspend context.
            combine(
                salesRepository.allSales,
                repairRepository.allRepairs,
                inventoryRepository.lowStockProducts,
                inventoryRepository.recentStockMovement,
                inventoryRepository.allProducts
            ) { sales, repairs, lowStock, stockMovements, products ->
                val startOfToday = getStartOfToday()
                val todaySales = sales.filter { it.dateTime >= startOfToday }
                val todayRevenueSum = todaySales.sumOf { it.totalAmount }
                
                val pendingRepairs = repairs.filter { it.status != "Collected" && it.status != "Completed" }
                
                // Formulate recent activities
                val activities = mutableListOf<ActivityItem>()
                sales.take(10).forEach {
                    activities.add(ActivityItem("New sale logged: GH₵${String.format("%.2f", it.totalAmount)}", it.dateTime, "SALE"))
                }
                repairs.take(10).forEach {
                    activities.add(ActivityItem("Repair #${it.id} (${it.brand} ${it.model}): Status updated to ${it.status}", it.updatedAt, "REPAIR"))
                }
                stockMovements.take(10).forEach { movement ->
                    val pName = products.find { it.id == movement.productId }?.name ?: "Product"
                    activities.add(ActivityItem("Stock movement: $pName (${if (movement.quantityChanged > 0) "+" else ""}${movement.quantityChanged})", movement.dateTime, "STOCK"))
                }
                val sortedActivities = activities.sortedByDescending { it.timestamp }.take(15)

                DashboardState(
                    todaySalesCount = todaySales.size,
                    todayRevenue = todayRevenueSum,
                    totalRepairsCount = repairs.size,
                    pendingRepairsCount = pendingRepairs.size,
                    lowStockItems = lowStock,
                    recentActivity = sortedActivities,
                    isLoading = false
                )
            }.collect { updatedState ->
                _state.value = updatedState
            }
        }
    }

    private fun getStartOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
