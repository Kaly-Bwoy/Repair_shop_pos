package com.mobilehub.pos.ui.screens.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobilehub.pos.data.local.entity.Category
import com.mobilehub.pos.data.local.entity.Product
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf<Product?>(null) }
    var showEditProductDialog by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Top Tabs: Products, Categories, Stock History
            TabRow(selectedTabIndex = state.activeTab) {
                Tab(
                    selected = state.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = { Text("Products", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = { Text("Categories", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    text = { Text("Stock Log", fontWeight = FontWeight.Bold) }
                )
            }

            when (state.activeTab) {
                0 -> {
                    // Products list screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search and Filter and Add
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                label = { Text("Search product SKU/name...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = { showAddProductDialog = true },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Product")
                            }
                        }

                        // Categories Horizontal Filter Row
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCategoryFilter == null,
                                    onClick = { viewModel.setCategoryFilter(null) },
                                    label = { Text("All Products") }
                                )
                            }
                            items(state.categoriesList) { cat ->
                                FilterChip(
                                    selected = state.selectedCategoryFilter == cat.id,
                                    onClick = { viewModel.setCategoryFilter(cat.id) },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        // Products list
                        val filteredProducts = state.productsList.filter {
                            (state.selectedCategoryFilter == null || it.categoryId == state.selectedCategoryFilter) &&
                            (it.name.contains(state.searchQuery, ignoreCase = true) || it.sku.contains(state.searchQuery, ignoreCase = true))
                        }

                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No products found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredProducts) { product ->
                                    ProductRow(
                                        product = product,
                                        categoryName = state.categoriesList.find { it.id == product.categoryId }?.name ?: "No Category",
                                        onRestock = { showRestockDialog = product },
                                        onEdit = { showEditProductDialog = product },
                                        onDelete = { viewModel.deleteProduct(product) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Categories list screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Product Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddCategoryDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Category")
                            }
                        }

                        if (state.categoriesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No categories configured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.categoriesList) { cat ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(cat.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            if (!cat.description.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Stock History List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Recent Stock Movements Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (state.stockHistoryList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No stock movements recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.stockHistoryList) { log ->
                                    val prodName = state.productsList.find { it.id == log.productId }?.name ?: "Unknown Product"
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.dateTime))

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prodName, fontWeight = FontWeight.Bold)
                                                Text("Type: ${log.changeType} | $dateStr", style = MaterialTheme.typography.bodySmall)
                                                if (!log.notes.isNullOrBlank()) {
                                                    Text("Notes: ${log.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            val signStr = if (log.quantityChanged > 0) "+" else ""
                                            val fontColor = if (log.quantityChanged > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            Text(
                                                text = "$signStr${log.quantityChanged}",
                                                fontWeight = FontWeight.Bold,
                                                color = fontColor,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        AddProductDialog(
            categories = state.categoriesList,
            onDismiss = { showAddProductDialog = false },
            onAdd = { name, sku, purchaseCost, sellingPrice, qty, threshold, catId, isPart ->
                viewModel.addProduct(name, sku, purchaseCost, sellingPrice, qty, threshold, catId, isPart)
                showAddProductDialog = false
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name, desc ->
                viewModel.addCategory(name, desc)
                showAddCategoryDialog = false
            }
        )
    }

    // Restock Dialog
    if (showRestockDialog != null) {
        val p = showRestockDialog!!
        RestockDialog(
            product = p,
            onDismiss = { showRestockDialog = null },
            onRestock = { qty, note ->
                viewModel.restockProduct(p.id, qty, note)
                showRestockDialog = null
            }
        )
    }

    // Edit Product Dialog
    if (showEditProductDialog != null) {
        val p = showEditProductDialog!!
        EditProductDialog(
            product = p,
            categories = state.categoriesList,
            onDismiss = { showEditProductDialog = null },
            onSave = { updatedProd, reason ->
                viewModel.updateProduct(updatedProd, reason)
                showEditProductDialog = null
            }
        )
    }
}
