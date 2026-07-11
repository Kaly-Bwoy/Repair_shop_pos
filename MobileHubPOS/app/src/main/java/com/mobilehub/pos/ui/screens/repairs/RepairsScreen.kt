package com.mobilehub.pos.ui.screens.repairs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Product
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.RepairPart
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairsScreen(
    viewModel: RepairsViewModel,
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRepairForDetail by remember { mutableStateOf<Repair?>(null) }

    val statusList = listOf("Received", "Diagnosing", "Waiting for parts", "Repairing", "Completed", "Collected")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repair Ticket Desk", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Ticket")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Horizontal Status Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedStatusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("All Tickets") }
                    )
                }
                items(statusList) { status ->
                    FilterChip(
                        selected = state.selectedStatusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(status) }
                    )
                }
            }

            // Repairs Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search by brand, model, serial...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tickets List
            val filteredRepairs = state.repairsList.filter {
                (state.selectedStatusFilter == null || it.status == state.selectedStatusFilter) &&
                (it.brand.contains(state.searchQuery, ignoreCase = true) ||
                 it.model.contains(state.searchQuery, ignoreCase = true) ||
                 (it.imeiSerial?.contains(state.searchQuery, ignoreCase = true) == true))
            }

            if (filteredRepairs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No repair tickets matching current criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredRepairs) { repair ->
                        val customerName = state.customersList.find { it.id == repair.customerId }?.name ?: "Unknown Customer"
                        RepairTicketRow(
                            repair = repair,
                            customerName = customerName,
                            onClick = {
                                selectedRepairForDetail = repair
                                viewModel.loadPartsForRepair(repair.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // New Repair Ticket Dialog
    if (showCreateDialog) {
        CreateRepairTicketDialog(
            customers = state.customersList,
            onDismiss = { showCreateDialog = false },
            onSave = { customerId, devType, brand, model, imei, pw, cond, fault, labor, notes ->
                viewModel.createRepairTicket(customerId, devType, brand, model, imei, pw, cond, fault, labor, notes)
                showCreateDialog = false
            }
        )
    }

    // Detail & Manage Dialog
    if (selectedRepairForDetail != null) {
        val currentRepairId = selectedRepairForDetail!!.id
        val repair = state.repairsList.find { it.id == currentRepairId } ?: selectedRepairForDetail!!
        val customer = state.customersList.find { it.id == repair.customerId }

        RepairDetailsDialog(
            repair = repair,
            customer = customer,
            partsUsed = state.selectedRepairParts,
            inventoryParts = state.inventoryPartsList,
            onDismiss = { selectedRepairForDetail = null },
            onStatusChange = { newStatus -> viewModel.updateRepairStatus(repair, newStatus) },
            onAddPart = { product, genericName, qty, price ->
                viewModel.addPartToRepair(repair.id, product, genericName, qty, price)
            },
            onRemovePart = { part -> viewModel.removePartFromRepair(part) },
            onAddPayment = { amount, method, payNotes ->
                viewModel.addRepairPayment(repair.id, amount, method, payNotes)
            }
        )
    }
}
