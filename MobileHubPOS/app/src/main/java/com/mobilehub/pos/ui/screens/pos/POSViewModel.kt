package com.mobilehub.pos.ui.screens.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.repository.CartItem
import com.mobilehub.pos.data.repository.CustomerRepository
import com.mobilehub.pos.data.repository.InventoryRepository
import com.mobilehub.pos.data.repository.SalesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class POSState(
    val cartList: List<CartItem> = emptyList(),
    val searchedProducts: List<Product> = emptyList(),
    val customersList: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val searchProductQuery: String = "",
    val searchCustomerQuery: String = "",
    val paymentMethod: String = "Cash",
    val checkoutDiscount: Double = 0.0,
    val showCheckoutSuccess: Boolean = false,
    val lastSaleId: Long? = null,
    val errorMessage: String? = null
)

class POSViewModel(
    private val salesRepository: SalesRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(POSState())
    val state: StateFlow<POSState> = _state.asStateFlow()

    init {
        // Load customers and listen to changes
        viewModelScope.launch {
            customerRepository.allCustomers.collect { list ->
                _state.update { it.copy(customersList = list) }
            }
        }
    }

    fun searchProduct(query: String) {
        _state.update { it.copy(searchProductQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchedProducts = emptyList()) }
            return
        }
        
        viewModelScope.launch {
            inventoryRepository.allProducts.collectLatest { list ->
                val matched = list.filter {
                    it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
                }
                _state.update { it.copy(searchedProducts = matched) }
            }
        }
    }

    fun searchProductBySku(sku: String) {
        viewModelScope.launch {
            val product = inventoryRepository.getProductBySku(sku)
            if (product != null) {
                addProductToCart(product)
                _state.update { it.copy(searchProductQuery = "") }
            } else {
                _state.update { it.copy(errorMessage = "Barcode/SKU not found: $sku") }
            }
        }
    }

    fun addProductToCart(product: Product) {
        if (product.stockQuantity <= 0) {
            _state.update { it.copy(errorMessage = "Product out of stock!") }
            return
        }

        val currentCart = _state.value.cartList.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val item = currentCart[index]
            if (item.quantity + 1 > product.stockQuantity) {
                _state.update { it.copy(errorMessage = "Cannot exceed available stock limit (${product.stockQuantity})") }
                return
            }
            currentCart[index] = item.copy(quantity = item.quantity + 1)
        } else {
            currentCart.add(CartItem(product = product, quantity = 1))
        }

        _state.update { it.copy(cartList = currentCart, searchedProducts = emptyList(), searchProductQuery = "") }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeProductFromCart(productId)
            return
        }

        val currentCart = _state.value.cartList.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }

        if (index != -1) {
            val item = currentCart[index]
            if (newQuantity > item.product.stockQuantity) {
                _state.update { it.copy(errorMessage = "Cannot exceed available stock (${item.product.stockQuantity})") }
                return
            }
            currentCart[index] = item.copy(quantity = newQuantity)
            _state.update { it.copy(cartList = currentCart) }
        }
    }

    fun updateCartItemDiscount(productId: Long, discountPerUnit: Double) {
        val currentCart = _state.value.cartList.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }

        if (index != -1) {
            val item = currentCart[index]
            if (discountPerUnit > item.product.sellingPrice) {
                _state.update { it.copy(errorMessage = "Discount cannot exceed price") }
                return
            }
            currentCart[index] = item.copy(discount = discountPerUnit)
            _state.update { it.copy(cartList = currentCart) }
        }
    }

    fun removeProductFromCart(productId: Long) {
        val currentCart = _state.value.cartList.filter { it.product.id != productId }
        _state.update { it.copy(cartList = currentCart) }
    }

    fun selectCustomer(customer: Customer?) {
        _state.update { it.copy(selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: String) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun setCheckoutDiscount(discount: Double) {
        _state.update { it.copy(checkoutDiscount = discount) }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccess() {
        _state.update { it.copy(showCheckoutSuccess = false, lastSaleId = null) }
    }

    fun checkoutCart() {
        val currentState = _state.value
        if (currentState.cartList.isEmpty()) {
            _state.update { it.copy(errorMessage = "Cart is empty") }
            return
        }

        val subtotal = currentState.cartList.sumOf { it.product.sellingPrice * it.quantity }
        val itemsDiscount = currentState.cartList.sumOf { it.discount * it.quantity }
        val finalDiscount = itemsDiscount + currentState.checkoutDiscount
        val total = (subtotal - finalDiscount).coerceAtLeast(0.0)

        viewModelScope.launch {
            try {
                val saleId = salesRepository.checkout(
                    customerId = currentState.selectedCustomer?.id,
                    subtotal = subtotal,
                    discount = finalDiscount,
                    tax = 0.0,
                    totalAmount = total,
                    paymentMethod = currentState.paymentMethod,
                    items = currentState.cartList
                )
                // Clear cart state on success
                _state.update {
                    it.copy(
                        cartList = emptyList(),
                        selectedCustomer = null,
                        checkoutDiscount = 0.0,
                        lastSaleId = saleId,
                        showCheckoutSuccess = true
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Checkout failed: ${e.message}") }
            }
        }
    }
}
