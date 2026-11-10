package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.data.model.MaintenanceLog
import com.example.data.model.ServiceReminder
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GarageViewModel,
    onSignOutRequested: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val maintenanceLogsState by viewModel.maintenanceLogsState.collectAsStateWithLifecycle()
    val remindersState by viewModel.serviceRemindersState.collectAsStateWithLifecycle()
    val selectedFamilyFilter by viewModel.selectedFamilyFilter.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog & sheet states
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var selectedVehicleForDetail by remember { mutableStateOf<Vehicle?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAddLogDialog by remember { mutableStateOf(false) }
    var targetVehicleForLog by remember { mutableStateOf<String?>(null) }

    var showAddReminderDialog by remember { mutableStateOf(false) }
    var targetVehicleForReminder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val vehicles = (vehiclesState as? UiState.Success)?.data ?: emptyList()
    val logs = (maintenanceLogsState as? UiState.Success)?.data ?: emptyList()
    val reminders = (remindersState as? UiState.Success)?.data ?: emptyList()

    // Filter vehicles by family member
    val filteredVehicles = if (selectedFamilyFilter.isNullOrBlank() || selectedFamilyFilter == "All") {
        vehicles
    } else {
        vehicles.filter { it.familyMember.contains(selectedFamilyFilter!!, ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Family Garage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fleet & Maintenance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            AuthManager.signOut(
                                credentialManager = credentialManager,
                                scope = coroutineScope,
                                onSignOutComplete = onSignOutRequested
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowForward,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Garage") },
                    label = { Text("Vehicles") },
                    modifier = Modifier.testTag("nav_vehicles")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Maintenance") },
                    label = { Text("Maintenance") },
                    modifier = Modifier.testTag("nav_maintenance")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Reminders") },
                    label = { Text("Reminders") },
                    modifier = Modifier.testTag("nav_reminders")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Insurance") },
                    label = { Text("Insurance") },
                    modifier = Modifier.testTag("nav_insurance")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddVehicleDialog = true
                        1 -> {
                            targetVehicleForLog = vehicles.firstOrNull()?.id
                            showAddLogDialog = true
                        }
                        2 -> {
                            targetVehicleForReminder = vehicles.firstOrNull()?.id
                            showAddReminderDialog = true
                        }
                        3 -> showAddVehicleDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("primary_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Family Member Profile Filter Bar
            val familyProfiles = remember(vehicles) {
                val members = vehicles.map { it.familyMember }.distinct().toMutableList()
                if (!members.contains("Wife's Car")) members.add("Wife")
                if (!members.contains("Kid's Car")) members.add("Kid")
                members
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedFamilyFilter == null || selectedFamilyFilter == "All",
                            onClick = { viewModel.selectFamilyFilter(null) },
                            label = { Text("All Family Cars (${vehicles.size})") },
                            modifier = Modifier.testTag("filter_all_cars")
                        )
                    }

                    items(familyProfiles) { profile ->
                        val count = vehicles.count { it.familyMember.contains(profile, ignoreCase = true) }
                        FilterChip(
                            selected = selectedFamilyFilter == profile,
                            onClick = { viewModel.selectFamilyFilter(profile) },
                            label = { Text("$profile ($count)") }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { showAddVehicleDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Car", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> GarageVehiclesTab(
                    vehicles = filteredVehicles,
                    allVehicles = vehicles,
                    logs = logs,
                    reminders = reminders,
                    onVehicleClick = { vehicle -> selectedVehicleForDetail = vehicle },
                    onAddLog = { v ->
                        targetVehicleForLog = v.id
                        showAddLogDialog = true
                    },
                    onAddReminder = { v ->
                        targetVehicleForReminder = v.id
                        showAddReminderDialog = true
                    },
                    onAddNewVehicle = { showAddVehicleDialog = true }
                )
                1 -> MaintenanceLogsTab(
                    vehicles = vehicles,
                    logs = logs,
                    onAddLog = { showAddLogDialog = true },
                    onDeleteLog = { logId -> viewModel.deleteMaintenanceLog(logId) }
                )
                2 -> ServiceRemindersTab(
                    vehicles = vehicles,
                    reminders = reminders,
                    onToggle = { id, currentStatus -> viewModel.toggleReminder(id, currentStatus) },
                    onDelete = { id -> viewModel.deleteServiceReminder(id) },
                    onAddReminder = { showAddReminderDialog = true }
                )
                3 -> InsuranceVaultTab(
                    vehicles = filteredVehicles,
                    onAddVehicle = { showAddVehicleDialog = true },
                    onEditVehicle = { v -> vehicleToEdit = v },
                    onShowMessage = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddEditVehicleDialog(
            vehicleToEdit = null,
            onDismiss = { showAddVehicleDialog = false },
            onSave = { v ->
                viewModel.addVehicle(v) { showAddVehicleDialog = false }
            }
        )
    }

    // Edit Vehicle Dialog
    if (vehicleToEdit != null) {
        AddEditVehicleDialog(
            vehicleToEdit = vehicleToEdit,
            onDismiss = { vehicleToEdit = null },
            onSave = { v ->
                viewModel.updateVehicle(v) { vehicleToEdit = null }
            }
        )
    }

    // Vehicle Detail Bottom Sheet
    if (selectedVehicleForDetail != null) {
        VehicleDetailBottomSheet(
            vehicle = selectedVehicleForDetail!!,
            sheetState = sheetState,
            onDismiss = { selectedVehicleForDetail = null },
            onEditVehicle = { v ->
                selectedVehicleForDetail = null
                vehicleToEdit = v
            },
            onDeleteVehicle = { v ->
                selectedVehicleForDetail = null
                viewModel.deleteVehicle(v)
            },
            onAddLogForVehicle = { v ->
                targetVehicleForLog = v.id
                showAddLogDialog = true
            },
            onAddReminderForVehicle = { v ->
                targetVehicleForReminder = v.id
                showAddReminderDialog = true
            },
            onShowMessage = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    // Add Maintenance Log Dialog
    if (showAddLogDialog) {
        AddEditMaintenanceLogDialog(
            vehicles = vehicles,
            initialVehicleId = targetVehicleForLog,
            onDismiss = { showAddLogDialog = false },
            onSave = { log ->
                viewModel.addMaintenanceLog(log) { showAddLogDialog = false }
            }
        )
    }

    // Add Service Reminder Dialog
    if (showAddReminderDialog) {
        AddEditServiceReminderDialog(
            vehicles = vehicles,
            initialVehicleId = targetVehicleForReminder,
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminder ->
                viewModel.addServiceReminder(reminder) { showAddReminderDialog = false }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: GARAGE VEHICLES
// -------------------------------------------------------------
@Composable
fun GarageVehiclesTab(
    vehicles: List<Vehicle>,
    allVehicles: List<Vehicle>,
    logs: List<MaintenanceLog>,
    reminders: List<ServiceReminder>,
    onVehicleClick: (Vehicle) -> Unit,
    onAddLog: (Vehicle) -> Unit,
    onAddReminder: (Vehicle) -> Unit,
    onAddNewVehicle: () -> Unit
) {
    val totalSpent = remember(logs) { logs.sumOf { it.cost } }
    val activeRemindersCount = remember(reminders) { reminders.count { !it.isCompleted } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Dashboard Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fleet Status & Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(title = "Vehicles", value = "${allVehicles.size}")
                        MetricItem(title = "Reminders", value = "$activeRemindersCount due")
                        MetricItem(title = "Total Spent", value = "$%,.0f".format(totalSpent))
                    }
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Vehicles Added Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add profiles for your car, wife's car, and kid's cars to start tracking VINs, plates, insurance & service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddNewVehicle,
                            modifier = Modifier.testTag("empty_add_vehicle_button")
                        ) {
                            Text("Add First Vehicle")
                        }
                    }
                }
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    onClick = { onVehicleClick(vehicle) },
                    onAddLog = { onAddLog(vehicle) },
                    onAddReminder = { onAddReminder(vehicle) }
                )
            }
        }
    }
}

@Composable
private fun MetricItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit,
    onAddLog: () -> Unit,
    onAddReminder: () -> Unit
) {
    val badgeColor = remember(vehicle.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(vehicle.colorHex))
        } catch (_: Exception) {
            Color(0xFF2563EB)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("vehicle_card_${vehicle.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile & Family Member Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = vehicle.familyMember,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Text(
                    text = "${vehicle.year}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Title
            Text(
                text = "${vehicle.make} ${vehicle.model}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (vehicle.nickname.isNotBlank()) {
                Text(
                    text = "\"${vehicle.nickname}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs grid: VIN, License Plate, Mileage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PLATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = vehicle.licensePlate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(
                        text = "VIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (vehicle.vin.length > 8) "...${vehicle.vin.takeLast(8)}" else vehicle.vin,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MILEAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%,d mi".format(vehicle.currentMileage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Insurance Summary Badge
            if (vehicle.insuranceCompany.isNotBlank() || vehicle.insurancePolicyNumber.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${vehicle.insuranceCompany} (${vehicle.insuranceDuration})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddLog,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onAddReminder,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: MAINTENANCE & REPAIRS
// -------------------------------------------------------------
@Composable
fun MaintenanceLogsTab(
    vehicles: List<Vehicle>,
    logs: List<MaintenanceLog>,
    onAddLog: () -> Unit,
    onDeleteLog: (String) -> Unit
) {
    var typeFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, typeFilter) {
        if (typeFilter == "ALL") logs else logs.filter { it.logType == typeFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "MAINTENANCE", "REPAIR", "INSPECTION").forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { typeFilter = type },
                    label = { Text(type) }
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Service Records Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keep track of oil changes, brake repairs, and maintenance across all family vehicles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = onAddLog) {
                        Text("Add Service Record")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    val vehicle = vehicles.find { it.id == log.vehicleId }
                    MaintenanceLogCard(
                        log = log,
                        vehicle = vehicle,
                        onDelete = { onDeleteLog(log.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenanceLogCard(
    log: MaintenanceLog,
    vehicle: Vehicle?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vehicle label & tag
                val vehicleLabel = vehicle?.let { "${it.familyMember}: ${it.year} ${it.make} ${it.model}" } ?: "Vehicle"
                Text(
                    text = vehicleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Cost
                Text(
                    text = "$%,.2f".format(log.cost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.serviceDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%,d mi".format(log.mileage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (log.serviceProvider.isNotBlank()) {
                Text(
                    text = "Shop: ${log.serviceProvider}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: SERVICE REMINDERS
// -------------------------------------------------------------
@Composable
fun ServiceRemindersTab(
    vehicles: List<Vehicle>,
    reminders: List<ServiceReminder>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAddReminder: () -> Unit
) {
    var statusFilter by remember { mutableStateOf("ACTIVE") }

    val filteredReminders = remember(reminders, statusFilter) {
        when (statusFilter) {
            "ACTIVE" -> reminders.filter { !it.isCompleted }
            "COMPLETED" -> reminders.filter { it.isCompleted }
            else -> reminders
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == "ACTIVE",
                onClick = { statusFilter = "ACTIVE" },
                label = { Text("Upcoming (${reminders.count { !it.isCompleted }})") }
            )
            FilterChip(
                selected = statusFilter == "COMPLETED",
                onClick = { statusFilter = "COMPLETED" },
                label = { Text("Completed (${reminders.count { it.isCompleted }})") }
            )
            FilterChip(
                selected = statusFilter == "ALL",
                onClick = { statusFilter = "ALL" },
                label = { Text("All (${reminders.size})") }
            )
        }

        if (filteredReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (statusFilter == "ACTIVE") "No Upcoming Reminders" else "No Reminders Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Schedule oil changes, tire rotations, inspections, or registration renewals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = onAddReminder) {
                        Text("Set Service Reminder")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReminders, key = { it.id }) { reminder ->
                    val vehicle = vehicles.find { it.id == reminder.vehicleId }
                    ServiceReminderCard(
                        reminder = reminder,
                        vehicle = vehicle,
                        onToggle = { onToggle(reminder.id, reminder.isCompleted) },
                        onDelete = { onDelete(reminder.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceReminderCard(
    reminder: ServiceReminder,
    vehicle: Vehicle?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val prioColor = when (reminder.priority) {
        "HIGH" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("reminder_checkbox_${reminder.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val vehicleLabel = vehicle?.let { "${it.familyMember}: ${it.make} ${it.model}" } ?: "Vehicle"
                    Text(
                        text = vehicleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = prioColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = reminder.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = prioColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = reminder.serviceType,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reminder.dueDate.isNotBlank()) {
                        Text(
                            text = "Due: ${reminder.dueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reminder.dueMileage > 0) {
                        Text(
                            text = "At: %,d mi".format(reminder.dueMileage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (reminder.notes.isNotBlank()) {
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: INSURANCE VAULT
// -------------------------------------------------------------
@Composable
fun InsuranceVaultTab(
    vehicles: List<Vehicle>,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        onShowMessage("$label copied")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Family Insurance Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Centralized policy numbers, duration, and renewal dates for all vehicles in the household.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No vehicle insurance policies logged yet.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddVehicle) {
                            Text("Add Vehicle with Insurance")
                        }
                    }
                }
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = vehicle.familyMember,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { onEditVehicle(vehicle) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Update Policy", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Insurance Provider & Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "INSURANCE COMPANY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (vehicle.insuranceCompany.isNotBlank()) vehicle.insuranceCompany else "Not specified",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DURATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (vehicle.insuranceDuration.isNotBlank()) vehicle.insuranceDuration else "6 Months",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Policy Number with quick copy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "POLICY NUMBER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (vehicle.insurancePolicyNumber.isNotBlank()) vehicle.insurancePolicyNumber else "No policy # entered",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (vehicle.insurancePolicyNumber.isNotBlank()) {
                                IconButton(
                                    onClick = { copyToClipboard("Policy Number", vehicle.insurancePolicyNumber) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Policy Number",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (vehicle.insuranceStartDate.isNotBlank() || vehicle.insuranceEndDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Policy Period: ${vehicle.insuranceStartDate} to ${vehicle.insuranceEndDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
