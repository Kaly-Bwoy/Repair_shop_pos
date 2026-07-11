package com.mobilehub.pos.ui.screens.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.repository.CartItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    viewModel: POSViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToCustomers: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showCustomerDialog by remember { mutableStateOf(false) }
    var manualBarcodeValue by remember { mutableStateOf("") }

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("Information") },
            text = { Text(state.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    if (state.showCheckoutSuccess) {
        ReceiptDialog(
            saleId = state.lastSaleId ?: 0,
            cartList = state.cartList, 
            customer = state.selectedCustomer,
            totalAmount = (state.cartList.sumOf { it.product.sellingPrice * it.quantity } - state.checkoutDiscount),
            paymentMethod = state.paymentMethod,
            onDismiss = {
                viewModel.dismissSuccess()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POS / Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCustomers) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main left/top panel: Cart and searching
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.3f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product search & simulated barcode scan bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchProductQuery,
                        onValueChange = { viewModel.searchProduct(it) },
                        label = { Text("Search accessory / phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = manualBarcodeValue,
                        onValueChange = { manualBarcodeValue = it },
                        label = { Text("Barcode SKU") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (manualBarcodeValue.isNotBlank()) {
                                    viewModel.searchProductBySku(manualBarcodeValue)
                                    manualBarcodeValue = ""
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                            }
                        },
                        modifier = Modifier.width(140.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Search Results Dropdown List
                if (state.searchedProducts.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LazyColumn(modifier = Modifier.maxHeightIn(max = 200.dp)) {
                            items(state.searchedProducts) { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addProductToCart(prod) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.SemiBold)
                                        Text("SKU: ${prod.sku} | Qty: ${prod.stockQuantity}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("GH₵${String.format("%.2f", prod.sellingPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Cart list
                Text(
                    text = "Current Cart (${state.cartList.sumOf { it.quantity }} items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (state.cartList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your checkout cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.cartList) { item ->
                            CartItemRow(
                                item = item,
                                onQuantityChange = { qty -> viewModel.updateCartItemQuantity(item.product.id, qty) },
                                onDiscountChange = { disc -> viewModel.updateCartItemDiscount(item.product.id, disc) },
                                onRemove = { viewModel.removeProductFromCart(item.product.id) }
                            )
                        }
                    }
                }
            }

            // Right panel: Payment summary and Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Checkout Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Select Customer Field
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomerDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.selectedCustomer?.name ?: "Walk-in Customer",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.selectedCustomer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }

                // Running Totals Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val subtotal = state.cartList.sumOf { it.product.sellingPrice * it.quantity }
                        val discount = state.cartList.sumOf { it.discount * it.quantity } + state.checkoutDiscount
                        val total = (subtotal - discount).coerceAtLeast(0.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text("GH₵${String.format("%.2f", subtotal)}", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discounts", style = MaterialTheme.typography.bodyMedium)
                            Text("-GH₵${String.format("%.2f", discount)}", color = MaterialTheme.colorScheme.error)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "GH₵${String.format("%.2f", total)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Payment Mode Selection
                Text("Payment Method", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Mobile Money", "Card").forEach { method ->
                        val selected = state.paymentMethod == method
                        Button(
                            onClick = { viewModel.setPaymentMethod(method) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(method, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Huge Checkout Button
                Button(
                    onClick = { viewModel.checkoutCart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("PAY & RECORD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerDialog) {
        Dialog(onDismissRequest = { showCustomerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Customer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = state.searchCustomerQuery,
                        onValueChange = { /* filter logic */ },
                        label = { Text("Filter Customer") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCustomer(null)
                                        showCustomerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Walk-in Customer", fontWeight = FontWeight.Bold)
                                if (state.selectedCustomer == null) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider()
                        }
                        items(state.customersList) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCustomer(customer)
                                        showCustomerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                    Text(customer.phone, style = MaterialTheme.typography.bodySmall)
                                }
                                if (state.selectedCustomer?.id == customer.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    Button(
                        onClick = { showCustomerDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
