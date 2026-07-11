package com.mobilehub.pos.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.local.entity.Expense
import com.mobilehub.pos.ui.screens.reports.ReportsState
import com.mobilehub.pos.ui.screens.reports.BestSellerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ReportsState(
    val dailyRevenue: Double = 0.0,
    val weeklyRevenue: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val repairIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val profitEstimate: Double = 0.0,
    val bestSellingProducts: List<BestSellerItem> = emptyList(),
    val expensesList: List<Expense> = emptyList(),
    val isLoading: Boolean = false
)

data class BestSellerItem(
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double
)

class ReportsViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        generateReports()
    }

    fun generateReports() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // SECURE & PERFORMANCE FIX: Offload heavy database calculations onto Dispatchers.IO 
            // to completely prevent Application Not Responding (ANR) lags on memory-constrained Samsung SM-T561 devices.
            val result = withContext(Dispatchers.IO) {
                // Fetch date ranges
                val startOfDay = getStartOfPeriod(Calendar.DAY_OF_YEAR)
                val startOfWeek = getStartOfPeriod(Calendar.WEEK_OF_YEAR)
                val startOfMonth = getStartOfPeriod(Calendar.MONTH)

                // Read SQLite tables
                val sales = db.saleDao().getAllSalesSync()
                val saleItems = db.saleDao().getAllSaleItemsSync()
                val repairs = db.repairDao().getAllRepairsSync()
                val repairParts = db.repairDao().getAllRepairPartsSync()
                val expenses = db.expenseDao().getAllExpensesSync()
                val products = db.productDao().getAllProductsSync()

                // 1. Calculate revenues
                val dailyRevenueSum = sales.filter { it.dateTime >= startOfDay }.sumOf { it.totalAmount }
                val weeklyRevenueSum = sales.filter { it.dateTime >= startOfWeek }.sumOf { it.totalAmount }
                val monthlyRevenueSum = sales.filter { it.dateTime >= startOfMonth }.sumOf { it.totalAmount }

                // 2. Calculate repair income (Collected repairs)
                val collectedRepairs = repairs.filter { it.status == "Collected" }
                val repairIncomeSum = collectedRepairs.sumOf { it.amountPaid }

                // 3. Expenses
                val expensesSum = expenses.sumOf { it.amount }

                // 4. Best Selling Products
                val bestSellers = saleItems
                    .groupBy { it.productId }
                    .map { (productId, items) ->
                        val prodName = items.firstOrNull()?.productName ?: "Product #$productId"
                        val qty = items.sumOf { it.quantity }
                        val rev = items.sumOf { it.totalPrice }
                        BestSellerItem(prodName, qty, rev)
                    }
                    .sortedByDescending { it.quantitySold }
                    .take(10)

                // 5. Profit Estimate calculations
                var totalPosProductCost = 0.0
                saleItems.forEach { item ->
                    val costPrice = products.find { it.id == item.productId }?.purchaseCost ?: 0.0
                    totalPosProductCost += (costPrice * item.quantity)
                }
                val posProfit = (sales.sumOf { it.totalAmount } - totalPosProductCost).coerceAtLeast(0.0)

                val repairLaborProfit = collectedRepairs.sumOf { it.laborCost }
                val repairPartsProfit = repairParts
                    .filter { part -> collectedRepairs.any { it.id == part.repairId } }
                    .sumOf { (it.sellingPrice - it.costPrice) * it.quantity }

                val totalProfitEstimate = (posProfit + repairLaborProfit + repairPartsProfit) - expensesSum

                ReportsState(
                    dailyRevenue = dailyRevenueSum,
                    weeklyRevenue = weeklyRevenueSum,
                    monthlyRevenue = monthlyRevenueSum,
                    repairIncome = repairIncomeSum,
                    totalExpenses = expensesSum,
                    profitEstimate = totalProfitEstimate,
                    bestSellingProducts = bestSellers,
                    expensesList = expenses,
                    isLoading = false
                )
            }

            _state.value = result
        }
    }

    fun addExpense(category: String, amount: Double, description: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.expenseDao().insertExpense(
                    Expense(category = category, amount = amount, description = description)
                )
            }
            generateReports() // Refresh calculations on Dispatchers.IO context
        }
    }

    private fun getStartOfPeriod(periodType: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (periodType == Calendar.WEEK_OF_YEAR) {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        } else if (periodType == Calendar.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}
