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
import com.example.data.model.ServiceReminder
import com.example.data.model.Vehicle

private val REMINDER_PRESETS = listOf(
    "Oil & Filter Change",
    "Tire Rotation",
    "Brake Inspection",
    "State Inspection Renewal",
    "Insurance Policy Renewal",
    "Registration Renewal",
    "Cabin Air Filter",
    "Coolant Flush"
)

private val PRIORITIES = listOf("LOW", "MEDIUM", "HIGH")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditServiceReminderDialog(
    vehicles: List<Vehicle>,
    initialVehicleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (ServiceReminder) -> Unit
) {
    var selectedVehicleId by remember {
        mutableStateOf(initialVehicleId ?: vehicles.firstOrNull()?.id ?: "")
    }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    val currentVehicle = vehicles.find { it.id == selectedVehicleId }

    var serviceType by remember { mutableStateOf("Oil & Filter Change") }
    var dueDate by remember { mutableStateOf("2025-06-01") }
    var dueMileageText by remember {
        val nextMileage = ((currentVehicle?.currentMileage ?: 0) + 5000)
        mutableStateOf(if (nextMileage > 5000) nextMileage.toString() else "40000")
    }
    var priority by remember { mutableStateOf("MEDIUM") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Upcoming Service Reminder",
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
                // Vehicle
                Text(
                    text = "Vehicle",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = vehicleMenuExpanded,
                    onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }
                ) {
                    val label = currentVehicle?.let {
                        "${it.familyMember}: ${it.year} ${it.make} ${it.model}"
                    } ?: "Select vehicle"

                    OutlinedTextField(
                        value = label,
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
                                text = { Text("${v.familyMember}: ${v.year} ${v.make} ${v.model}") },
                                onClick = {
                                    selectedVehicleId = v.id
                                    dueMileageText = (v.currentMileage + 5000).toString()
                                    vehicleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Presets
                Text(
                    text = "Service Type",
                    style = MaterialTheme.typography.labelSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    REMINDER_PRESETS.take(6).forEach { preset ->
                        FilterChip(
                            selected = serviceType == preset,
                            onClick = { serviceType = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    label = { Text("Service / Reminder Title") },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f).testTag("reminder_date_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueMileageText,
                        onValueChange = { dueMileageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Due Mileage") },
                        modifier = Modifier.weight(1f).testTag("reminder_mileage_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRIORITIES.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reminder Notes (e.g. 5W-30 Synthetic, coupon code)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
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
                    if (serviceType.isBlank()) {
                        errorMessage = "Please enter service type"
                        return@Button
                    }
                    val dueMileage = dueMileageText.toIntOrNull() ?: 0
                    val reminder = ServiceReminder(
                        vehicleId = selectedVehicleId,
                        serviceType = serviceType.trim(),
                        dueDate = dueDate.trim(),
                        dueMileage = dueMileage,
                        isCompleted = false,
                        priority = priority,
                        notes = notes.trim()
                    )
                    onSave(reminder)
                },
                modifier = Modifier.testTag("save_reminder_button")
            ) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
