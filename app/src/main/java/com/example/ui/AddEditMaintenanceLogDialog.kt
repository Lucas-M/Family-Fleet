package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MaintenanceLog
import com.example.data.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SERVICE_PRESETS = listOf(
    "Oil & Filter Change",
    "Brake Pads Replacement",
    "Tire Rotation & Balance",
    "Air & Cabin Filters",
    "Battery Replacement",
    "Transmission Fluid",
    "Spark Plugs",
    "Alternator Repair",
    "Wheel Alignment",
    "State Safety Inspection"
)

private val LOG_TYPES = listOf("MAINTENANCE", "REPAIR", "INSPECTION")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaintenanceLogDialog(
    vehicles: List<Vehicle>,
    initialVehicleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (MaintenanceLog) -> Unit
) {
    val defaultDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedVehicleId by remember {
        mutableStateOf(initialVehicleId ?: vehicles.firstOrNull()?.id ?: "")
    }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    val currentVehicle = vehicles.find { it.id == selectedVehicleId }

    var title by remember { mutableStateOf("Oil & Filter Change") }
    var logType by remember { mutableStateOf("MAINTENANCE") }
    var serviceDate by remember { mutableStateOf(defaultDate) }
    var mileageText by remember { mutableStateOf(currentVehicle?.currentMileage?.toString() ?: "0") }
    var costText by remember { mutableStateOf("75.00") }
    var serviceProvider by remember { mutableStateOf("Local Service Center") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Maintenance or Repair",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vehicle Picker Dropdown
                Text(
                    text = "Vehicle",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = vehicleMenuExpanded,
                    onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }
                ) {
                    val selectedVehicleLabel = currentVehicle?.let {
                        "${it.familyMember} - ${it.year} ${it.make} ${it.model}"
                    } ?: "Select vehicle"

                    OutlinedTextField(
                        value = selectedVehicleLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleMenuExpanded,
                        onDismissRequest = { vehicleMenuExpanded = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.familyMember}: ${v.year} ${v.make} ${v.model} (${v.licensePlate})") },
                                onClick = {
                                    selectedVehicleId = v.id
                                    if (mileageText == "0" && v.currentMileage > 0) {
                                        mileageText = v.currentMileage.toString()
                                    }
                                    vehicleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Type Chips
                Text(
                    text = "Record Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LOG_TYPES.forEach { type ->
                        FilterChip(
                            selected = logType == type,
                            onClick = { logType = type },
                            label = { Text(type) }
                        )
                    }
                }

                // Title Presets
                Text(
                    text = "Common Services",
                    style = MaterialTheme.typography.labelSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SERVICE_PRESETS.take(6).forEach { preset ->
                        FilterChip(
                            selected = title == preset,
                            onClick = { title = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service Description / Repair Title") },
                    modifier = Modifier.fillMaxWidth().testTag("log_title_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serviceDate,
                        onValueChange = { serviceDate = it },
                        label = { Text("Service Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f).testTag("log_date_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mileageText,
                        onValueChange = { mileageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Odometer Mileage") },
                        modifier = Modifier.weight(1f).testTag("log_mileage_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Cost ($)") },
                        modifier = Modifier.weight(1f).testTag("log_cost_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serviceProvider,
                        onValueChange = { serviceProvider = it },
                        label = { Text("Provider / Shop") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Detailed Notes & Part Numbers") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicleId.isBlank()) {
                        errorMessage = "Please select a vehicle"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "Please enter service title"
                        return@Button
                    }
                    val mileage = mileageText.toIntOrNull() ?: 0
                    val cost = costText.toDoubleOrNull() ?: 0.0

                    val log = MaintenanceLog(
                        vehicleId = selectedVehicleId,
                        title = title.trim(),
                        logType = logType,
                        serviceDate = serviceDate.trim(),
                        mileage = mileage,
                        cost = cost,
                        serviceProvider = serviceProvider.trim(),
                        notes = notes.trim()
                    )
                    onSave(log)
                },
                modifier = Modifier.testTag("save_log_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
