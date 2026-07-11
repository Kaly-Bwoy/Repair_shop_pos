package com.mobilehub.pos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobilehub.pos.MobileHubApplication
import com.mobilehub.pos.ui.theme.MobileHubTheme
import com.mobilehub.pos.ui.screens.dashboard.DashboardScreen
import com.mobilehub.pos.ui.screens.dashboard.DashboardViewModel
import com.mobilehub.pos.ui.screens.pos.POSScreen
import com.mobilehub.pos.ui.screens.pos.POSViewModel
import com.mobilehub.pos.ui.screens.inventory.InventoryScreen
import com.mobilehub.pos.ui.screens.inventory.InventoryViewModel
import com.mobilehub.pos.ui.screens.repairs.RepairsScreen
import com.mobilehub.pos.ui.screens.repairs.RepairsViewModel
import com.mobilehub.pos.ui.screens.customers.CustomersScreen
import com.mobilehub.pos.ui.screens.customers.CustomerViewModel
import com.mobilehub.pos.ui.screens.reports.ReportsScreen
import com.mobilehub.pos.ui.screens.reports.ReportsViewModel
import com.mobilehub.pos.ui.screens.settings.SettingsScreen
import com.mobilehub.pos.ui.screens.settings.SettingsViewModel
import com.mobilehub.pos.ui.screens.setup.SetupWizardScreen
import com.mobilehub.pos.util.SecurityUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as MobileHubApplication

        // Custom Factories for viewmodels to adhere to clean architecture MVVM
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                        DashboardViewModel(app.salesRepository, app.repairRepository, app.inventoryRepository) as T
                    }
                    modelClass.isAssignableFrom(POSViewModel::class.java) -> {
                        POSViewModel(app.salesRepository, app.inventoryRepository, app.customerRepository) as T
                    }
                    modelClass.isAssignableFrom(InventoryViewModel::class.java) -> {
                        InventoryViewModel(app.inventoryRepository) as T
                    }
                    modelClass.isAssignableFrom(RepairsViewModel::class.java) -> {
                        RepairsViewModel(app.repairRepository, app.customerRepository, app.inventoryRepository) as T
                    }
                    modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                        CustomerViewModel(app.customerRepository) as T
                    }
                    modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                        ReportsViewModel(app.database) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        // SECURE UPDATE: Pass app.database directly to support advanced store profiles & PIN lookups
                        SettingsViewModel(app.database, app.backupRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        setContent {
            MobileHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppLayout(factory)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object POS : Screen("pos", "POS Checkout", Icons.Default.ShoppingCart)
    object Repairs : Screen("repairs", "Repairs Desk", Icons.Default.Build)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Inventory2)
    object Customers : Screen("customers", "Customers", Icons.Default.Group)
    object Reports : Screen("reports", "Reports & Expenses", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppLayout(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as MobileHubApplication
    val adminDao = app.database.adminDao()

    var storeProfileState by remember { mutableStateOf<com.mobilehub.pos.data.local.entity.StoreProfile?>(null) }
    var isCheckingProfile by remember { mutableStateOf(true) }

    // SECURE INTERCEPT STATES
    var showPinDialog by remember { mutableStateOf(false) }
    var pinTargetRoute by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }

    // Read profile on start
    LaunchedEffect(Unit) {
        val profile = adminDao.getStoreProfile()
        storeProfileState = profile
        isCheckingProfile = false
    }

    if (isCheckingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Redirect to Wizard if Profile has not been created yet
    if (storeProfileState == null) {
        SetupWizardScreen(
            onSetupComplete = { profile, user ->
                scope.launch {
                    adminDao.insertStoreProfile(profile)
                    adminDao.insertUser(user)
                    storeProfileState = profile
                }
            }
        )
        return
    }

    // Profile exists, load normal drawer layout
    val menuItems = listOf(
        Screen.Dashboard,
        Screen.POS,
        Screen.Repairs,
        Screen.Inventory,
        Screen.Customers,
        Screen.Reports,
        Screen.Settings
    )

    var currentRoute by remember { mutableStateOf(Screen.Dashboard.route) }

    // SECURE PIN DIALOG GATE
    if (showPinDialog) {
        Dialog(onDismissRequest = { 
            showPinDialog = false
            enteredPin = ""
            pinErrorText = null
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Administrator Access Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Entering protected screen. Please type your 4-digit Store Admin PIN to confirm authorization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4) enteredPin = it },
                        label = { Text("Enter 4-Digit Admin PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(180.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (pinErrorText != null) {
                        Text(
                            text = pinErrorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            showPinDialog = false
                            enteredPin = ""
                            pinErrorText = null
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val hashedInput = SecurityUtils.hashPin(enteredPin)
                            val actualHash = storeProfileState?.adminPin ?: ""
                            if (hashedInput == actualHash) {
                                // PIN MATCHED! Grant access
                                showPinDialog = false
                                enteredPin = ""
                                pinErrorText = null
                                
                                val target = pinTargetRoute
                                if (target != null) {
                                    currentRoute = target
                                    scope.launch { drawerState.close() }
                                    navController.navigate(target) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else {
                                pinErrorText = "Invalid Admin PIN! Access denied."
                                enteredPin = ""
                            }
                        }) {
                            Text("Authorize")
                        }
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = storeProfileState?.storeName ?: "MobileHub POS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                menuItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title, fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            // SECURE NAVIGATION REDIRECT GATE
                            val needsPin = screen == Screen.Reports || screen == Screen.Settings
                            if (needsPin && currentRoute != screen.route) {
                                pinTargetRoute = screen.route
                                showPinDialog = true
                            } else {
                                currentRoute = screen.route
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(Screen.Dashboard.route) {
                    val vm: DashboardViewModel = viewModel(factory = viewModelFactory)
                    DashboardScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateToPOS = {
                            currentRoute = Screen.POS.route
                            navController.navigate(Screen.POS.route)
                        },
                        onNavigateToRepairs = {
                            currentRoute = Screen.Repairs.route
                            navController.navigate(Screen.Repairs.route)
                        },
                        onNavigateToInventory = {
                            currentRoute = Screen.Inventory.route
                            navController.navigate(Screen.Inventory.route)
                        }
                    )
                }
                composable(Screen.POS.route) {
                    val vm: POSViewModel = viewModel(factory = viewModelFactory)
                    POSScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateToCustomers = {
                            currentRoute = Screen.Customers.route
                            navController.navigate(Screen.Customers.route)
                        }
                    )
                }
                composable(Screen.Repairs.route) {
                    val vm: RepairsViewModel = viewModel(factory = viewModelFactory)
                    RepairsScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Inventory.route) {
                    val vm: InventoryViewModel = viewModel(factory = viewModelFactory)
                    InventoryScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Customers.route) {
                    val vm: CustomerViewModel = viewModel(factory = viewModelFactory)
                    CustomersScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Reports.route) {
                    val vm: ReportsViewModel = viewModel(factory = viewModelFactory)
                    ReportsScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Settings.route) {
                    val vm: SettingsViewModel = viewModel(factory = viewModelFactory)
                    SettingsScreen(
                        viewModel = vm,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
