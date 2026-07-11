package com.mobilehub.pos.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.pos.data.local.entity.StoreProfile
import com.mobilehub.pos.data.local.entity.User
import com.mobilehub.pos.util.SecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onSetupComplete: (StoreProfile, User) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("GH₵") }
    var email by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf("0.0") }
    var adminPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var logoPathSimulated by remember { mutableStateOf<String?>(null) }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isCurrencyExpanded by remember { mutableStateOf(false) }

    val currencies = listOf("GH₵ (GHS)", "$ (USD)", "₦ (NGN)", "€ (EUR)", "£ (GBP)", "Ksh (KES)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobileHub POS Store Setup", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "Welcome to MobileHub POS!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Configure your local offline business parameters to get started. These details will be printed on all your sales invoices, repair receipts, and business reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Form inputs
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Store Name *") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Store Phone Number *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Store Physical Address *") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Business Email (Optional)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Currency dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        label = { Text("Currency") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCurrencyExpanded = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = isCurrencyExpanded,
                        onDismissRequest = { isCurrencyExpanded = false }
                    ) {
                        currencies.forEach { curr ->
                            val symbolOnly = curr.substringBefore(" ")
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    currency = symbolOnly
                                    isCurrencyExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = taxRate,
                    onValueChange = { taxRate = it },
                    label = { Text("Sales Tax Rate %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Security Settings (Protect Settings & Reports)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = adminPin,
                    onValueChange = { if (it.length <= 4) adminPin = it },
                    label = { Text("Admin PIN *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Image Logo simulated uploader button
            Button(
                onClick = { logoPathSimulated = "default_repair_logo_simulated.png" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (logoPathSimulated == null) "Upload Store Logo (Simulated)" else "Logo Selected: $logoPathSimulated",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (storeName.isBlank() || phone.isBlank() || address.isBlank() || adminPin.isBlank()) {
                        errorMsg = "Please fill all required (*) fields!"
                    } else if (adminPin != confirmPin) {
                        errorMsg = "Admin PINs do not match!"
                    } else if (adminPin.length < 4) {
                        errorMsg = "PIN must be exactly 4 digits long!"
                    } else {
                        errorMsg = null
                        // SECURE ENHANCEMENT: Cryptographic PIN hashing using native SHA-256
                        val hashedPin = SecurityUtils.hashPin(adminPin)
                        val profile = StoreProfile(
                            storeName = storeName,
                            logoPath = logoPathSimulated,
                            phone = phone,
                            address = address,
                            currency = currency,
                            email = email.ifBlank { null },
                            taxRate = taxRate.toDoubleOrNull() ?: 0.0,
                            adminPin = hashedPin
                        )
                        val adminUser = User(
                            username = "Admin",
                            role = "Admin",
                            passwordPin = hashedPin
                        )
                        onSetupComplete(profile, adminUser)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREATE PROFILE & LAUNCH", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
