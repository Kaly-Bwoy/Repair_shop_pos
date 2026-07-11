package com.mobilehub.pos.ui.screens.repairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.RepairPart
import com.mobilehub.pos.data.repository.CustomerRepository
import com.mobilehub.pos.data.repository.InventoryRepository
import com.mobilehub.pos.data.repository.RepairRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RepairsState(
    val repairsList: List<Repair> = emptyList(),
    val customersList: List<Customer> = emptyList(),
    val inventoryPartsList: List<Product> = emptyList(), // For adding parts
    val selectedRepairParts: List<RepairPart> = emptyList(), // For detail view
    val isLoading: Boolean = false,
    val selectedStatusFilter: String? = null,
    val searchQuery: String = ""
)

class RepairsViewModel(
    private val repairRepository: RepairRepository,
    private val customerRepository: CustomerRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RepairsState())
    val state: StateFlow<RepairsState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            combine(
                repairRepository.allRepairs,
                customerRepository.allCustomers,
                inventoryRepository.allProducts
            ) { repairs, customers, inventory ->
                RepairsState(
                    repairsList = repairs,
                    customersList = customers,
                    inventoryPartsList = inventory.filter { it.isPart || it.stockQuantity > 0 },
                    selectedRepairParts = _state.value.selectedRepairParts,
                    isLoading = false,
                    selectedStatusFilter = _state.value.selectedStatusFilter,
                    searchQuery = _state.value.searchQuery
                )
            }.collect { updatedState ->
                _state.value = updatedState
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _state.update { it.copy(selectedStatusFilter = status) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun loadPartsForRepair(repairId: Long) {
        viewModelScope.launch {
            repairRepository.getRepairPartsForRepair(repairId).collect { parts ->
                _state.update { it.copy(selectedRepairParts = parts) }
            }
        }
    }

    fun createRepairTicket(
        customerId: Long,
        deviceType: String,
        brand: String,
        model: String,
        imeiSerial: String?,
        passwordPattern: String?,
        conditionNotes: String?,
        faultDescription: String,
        laborCost: Double,
        notes: String?,
        imagePath1: String? = null
    ) {
        viewModelScope.launch {
            val repair = Repair(
                customerId = customerId,
                deviceType = deviceType,
                brand = brand,
                model = model,
                imeiSerial = imeiSerial,
                passwordPattern = passwordPattern,
                conditionNotes = conditionNotes,
                faultDescription = faultDescription,
                status = "Received",
                laborCost = laborCost,
                totalEstimatedCost = laborCost,
                finalCost = laborCost,
                notes = notes,
                imagePath1 = imagePath1
            )
            repairRepository.createRepair(repair)
        }
    }

    fun updateRepairStatus(repair: Repair, newStatus: String) {
        viewModelScope.launch {
            repairRepository.updateRepair(repair.copy(status = newStatus))
        }
    }

    fun addPartToRepair(repairId: Long, product: Product?, genericPartName: String?, quantity: Int, customPrice: Double?) {
        viewModelScope.launch {
            val partName = product?.name ?: genericPartName ?: "Replacement Part"
            val costPrice = product?.purchaseCost ?: 0.0
            val sellingPrice = customPrice ?: product?.sellingPrice ?: 0.0
            val productId = product?.id

            repairRepository.addRepairPart(
                repairId = repairId,
                productId = productId,
                partName = partName,
                quantity = quantity,
                costPrice = costPrice,
                sellingPrice = sellingPrice
            )
            // Reload details
            loadPartsForRepair(repairId)
        }
    }

    fun removePartFromRepair(part: RepairPart) {
        viewModelScope.launch {
            repairRepository.removeRepairPart(part)
            // Reload details
            loadPartsForRepair(part.repairId)
        }
    }

    fun addRepairPayment(repairId: Long, amount: Double, paymentMethod: String, notes: String?) {
        viewModelScope.launch {
            repairRepository.addRepairPayment(repairId, amount, paymentMethod, notes)
        }
    }
}
