package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Vehicle

private val COLOR_OPTIONS = listOf(
    "#2563EB", // Blue
    "#0D9488", // Teal
    "#059669", // Emerald
    "#D97706", // Amber
    "#DC2626", // Red
    "#7C3AED", // Purple
    "#475569"  // Slate
)

private val FAMILY_PRESETS = listOf(
    "My Car",
    "Wife's Car",
    "Kid's Car (Alex)",
    "Kid's Car (Emma)"
)

private val DURATION_PRESETS = listOf(
    "6 Months",
    "1 Year",
    "Monthly"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditVehicleDialog(
    vehicleToEdit: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var familyMember by remember { mutableStateOf(vehicleToEdit?.familyMember ?: "My Car") }
    var nickname by remember { mutableStateOf(vehicleToEdit?.nickname ?: "") }
    var make by remember { mutableStateOf(vehicleToEdit?.make ?: "") }
    var model by remember { mutableStateOf(vehicleToEdit?.model ?: "") }
    var yearText by remember { mutableStateOf(vehicleToEdit?.year?.toString() ?: "2023") }
    var vin by remember { mutableStateOf(vehicleToEdit?.vin ?: "") }
    var licensePlate by remember { mutableStateOf(vehicleToEdit?.licensePlate ?: "") }
    var state by remember { mutableStateOf(vehicleToEdit?.state ?: "CA") }
    var mileageText by remember { mutableStateOf(vehicleToEdit?.currentMileage?.toString() ?: "35000") }
    var insuranceCompany by remember { mutableStateOf(vehicleToEdit?.insuranceCompany ?: "State Farm") }
    var insurancePolicyNumber by remember { mutableStateOf(vehicleToEdit?.insurancePolicyNumber ?: "") }
    var insuranceDuration by remember { mutableStateOf(vehicleToEdit?.insuranceDuration ?: "6 Months") }
    var insuranceStartDate by remember { mutableStateOf(vehicleToEdit?.insuranceStartDate ?: "2024-01-01") }
    var insuranceEndDate by remember { mutableStateOf(vehicleToEdit?.insuranceEndDate ?: "2024-07-01") }
    var selectedColor by remember { mutableStateOf(vehicleToEdit?.colorHex ?: "#2563EB") }
    var notes by remember { mutableStateOf(vehicleToEdit?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (vehicleToEdit == null) "Add Family Vehicle" else "Edit Vehicle",
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
                // Family Member Label
                Text(
                    text = "Family Profile / Owner",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FAMILY_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = familyMember == preset,
                            onClick = { familyMember = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                OutlinedTextField(
                    value = familyMember,
                    onValueChange = { familyMember = it },
                    label = { Text("Profile Label (e.g. Wife, Kid, Me)") },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_profile_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (optional, e.g. Daily Commuter)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Vehicle Specs
                Text(
                    text = "Vehicle Information",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f).testTag("vehicle_year_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make") },
                        placeholder = { Text("Toyota") },
                        modifier = Modifier.weight(1.5f).testTag("vehicle_make_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        placeholder = { Text("RAV4") },
                        modifier = Modifier.weight(1.5f).testTag("vehicle_model_input"),
                        singleLine = true
                    )
                }

                // VIN & License Plate
                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.uppercase() },
                    label = { Text("VIN# (Vehicle Identification Number)") },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_vin_input"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase() },
                        label = { Text("License Plate#") },
                        modifier = Modifier.weight(2f).testTag("vehicle_plate_input"),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it.uppercase() },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Current Mileage (Miles)") },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_mileage_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Insurance Details
                Text(
                    text = "Insurance Policy Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = insuranceCompany,
                    onValueChange = { insuranceCompany = it },
                    label = { Text("Insurance Company") },
                    placeholder = { Text("State Farm, Geico, Progressive...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = insurancePolicyNumber,
                    onValueChange = { insurancePolicyNumber = it },
                    label = { Text("Insurance Policy Number") },
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_policy_input"),
                    singleLine = true
                )

                Text(
                    text = "Insurance Duration",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATION_PRESETS.forEach { dur ->
                        FilterChip(
                            selected = insuranceDuration == dur,
                            onClick = { insuranceDuration = dur },
                            label = { Text(dur) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = insuranceStartDate,
                        onValueChange = { insuranceStartDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = insuranceEndDate,
                        onValueChange = { insuranceEndDate = it },
                        label = { Text("End / Renewal Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Color accent
                Text(
                    text = "Profile Accent Color",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    COLOR_OPTIONS.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (tire sizes, oil viscosity, etc.)") },
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
                    val year = yearText.toIntOrNull() ?: 2024
                    val mileage = mileageText.toIntOrNull() ?: 0

                    if (familyMember.isBlank()) {
                        errorMessage = "Please enter family member profile"
                        return@Button
                    }
                    if (make.isBlank() || model.isBlank()) {
                        errorMessage = "Please enter make and model"
                        return@Button
                    }
                    if (vin.isBlank()) {
                        errorMessage = "Please enter VIN#"
                        return@Button
                    }
                    if (licensePlate.isBlank()) {
                        errorMessage = "Please enter license plate#"
                        return@Button
                    }

                    val vehicle = (vehicleToEdit ?: Vehicle()).copy(
                        familyMember = familyMember.trim(),
                        nickname = nickname.trim(),
                        make = make.trim(),
                        model = model.trim(),
                        year = year,
                        vin = vin.trim().uppercase(),
                        licensePlate = licensePlate.trim().uppercase(),
                        state = state.trim().uppercase(),
                        currentMileage = mileage,
                        insuranceCompany = insuranceCompany.trim(),
                        insurancePolicyNumber = insurancePolicyNumber.trim(),
                        insuranceDuration = insuranceDuration.trim(),
                        insuranceStartDate = insuranceStartDate.trim(),
                        insuranceEndDate = insuranceEndDate.trim(),
                        colorHex = selectedColor,
                        notes = notes.trim()
                    )
                    onSave(vehicle)
                },
                modifier = Modifier.testTag("save_vehicle_button")
            ) {
                Text(if (vehicleToEdit == null) "Add Vehicle" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
