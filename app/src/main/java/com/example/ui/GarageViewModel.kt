package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.MaintenanceLog
import com.example.data.model.ServiceReminder
import com.example.data.model.Vehicle
import com.example.data.repository.GarageRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class GarageViewModel(
    private val repository: GarageRepository
) : ViewModel() {

    val vehiclesState: StateFlow<UiState<List<Vehicle>>> = repository.observeVehicles()
        .map<List<Vehicle>, UiState<List<Vehicle>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load vehicles"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val maintenanceLogsState: StateFlow<UiState<List<MaintenanceLog>>> = repository.observeMaintenanceLogs()
        .map<List<MaintenanceLog>, UiState<List<MaintenanceLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load maintenance logs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val serviceRemindersState: StateFlow<UiState<List<ServiceReminder>>> = repository.observeServiceReminders()
        .map<List<ServiceReminder>, UiState<List<ServiceReminder>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load reminders"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedVehicleId = MutableStateFlow<String?>(null)
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    private val _selectedFamilyFilter = MutableStateFlow<String?>(null)
    val selectedFamilyFilter: StateFlow<String?> = _selectedFamilyFilter.asStateFlow()

    private val _eventMessage = MutableSharedFlow<String>()
    val eventMessage: SharedFlow<String> = _eventMessage.asSharedFlow()

    fun selectVehicle(vehicleId: String?) {
        _selectedVehicleId.value = vehicleId
    }

    fun selectFamilyFilter(familyFilter: String?) {
        _selectedFamilyFilter.value = familyFilter
    }

    fun addVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addVehicle(vehicle)
            if (result.isSuccess) {
                _eventMessage.emit("${vehicle.familyMember} vehicle added successfully")
                onComplete()
            } else {
                _eventMessage.emit("Failed to add vehicle: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateVehicle(vehicle)
            if (result.isSuccess) {
                _eventMessage.emit("${vehicle.make} ${vehicle.model} updated")
                onComplete()
            } else {
                _eventMessage.emit("Failed to update vehicle: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteVehicle(vehicle.id)
            if (result.isSuccess) {
                if (_selectedVehicleId.value == vehicle.id) {
                    _selectedVehicleId.value = null
                }
                _eventMessage.emit("${vehicle.make} ${vehicle.model} removed from garage")
                onComplete()
            } else {
                _eventMessage.emit("Failed to delete vehicle: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun addMaintenanceLog(log: MaintenanceLog, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addMaintenanceLog(log)
            if (result.isSuccess) {
                _eventMessage.emit("Maintenance record saved")
                onComplete()
            } else {
                _eventMessage.emit("Failed to save log: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun deleteMaintenanceLog(logId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteMaintenanceLog(logId)
            if (result.isSuccess) {
                _eventMessage.emit("Log removed")
                onComplete()
            } else {
                _eventMessage.emit("Failed to delete log: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun addServiceReminder(reminder: ServiceReminder, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addServiceReminder(reminder)
            if (result.isSuccess) {
                _eventMessage.emit("Service reminder created")
                onComplete()
            } else {
                _eventMessage.emit("Failed to create reminder: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun toggleReminder(reminderId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentStatus
            val result = repository.toggleReminderCompletion(reminderId, newStatus)
            if (result.isSuccess) {
                val label = if (newStatus) "Reminder marked completed" else "Reminder reopened"
                _eventMessage.emit(label)
            } else {
                _eventMessage.emit("Failed to update reminder status")
            }
        }
    }

    fun deleteServiceReminder(reminderId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteServiceReminder(reminderId)
            if (result.isSuccess) {
                _eventMessage.emit("Reminder removed")
                onComplete()
            } else {
                _eventMessage.emit("Failed to delete reminder")
            }
        }
    }

    class Factory(private val repository: GarageRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GarageViewModel::class.java)) {
                return GarageViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
