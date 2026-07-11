package com.mobilehub.pos.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.Sale
import com.mobilehub.pos.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerState(
    val customersList: List<Customer> = emptyList(),
    val searchedCustomers: List<Customer> = emptyList(),
    val selectedCustomerPurchaseHistory: List<Sale> = emptyList(),
    val selectedCustomerRepairHistory: List<Repair> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class CustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerState())
    val state: StateFlow<CustomerState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            customerRepository.allCustomers.collect { list ->
                _state.update {
                    it.copy(
                        customersList = list,
                        searchedCustomers = if (_state.value.searchQuery.isBlank()) list else _state.value.searchedCustomers,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchCustomers(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchedCustomers = _state.value.customersList) }
            return
        }

        viewModelScope.launch {
            customerRepository.searchCustomers(query).collectLatest { list ->
                _state.update { it.copy(searchedCustomers = list) }
            }
        }
    }

    fun addCustomer(name: String, phone: String, address: String?, email: String?) {
        viewModelScope.launch {
            val customer = Customer(name = name, phone = phone, address = address, email = email)
            customerRepository.insertCustomer(customer)
        }
    }

    fun loadCustomerHistory(customerId: Long) {
        viewModelScope.launch {
            combine(
                customerRepository.getCustomerPurchaseHistory(customerId),
                customerRepository.getCustomerRepairHistory(customerId)
            ) { purchases, repairs ->
                Pair(purchases, repairs)
            }.collect { (purchases, repairs) ->
                _state.update {
                    it.copy(
                        selectedCustomerPurchaseHistory = purchases,
                        selectedCustomerRepairHistory = repairs
                    )
                }
            }
        }
    }
}
