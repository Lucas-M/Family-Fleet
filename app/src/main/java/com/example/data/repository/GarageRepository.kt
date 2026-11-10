package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.MaintenanceLog
import com.example.data.model.ServiceReminder
import com.example.data.model.Vehicle
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GarageRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- VEHICLES ---

    fun observeVehicles(): Flow<List<Vehicle>> = flow {
        val uid = requireUserId()
        val query = db.collection("vehicles").whereEqualTo("userId", uid)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Vehicle::class.java).sortedByDescending { it.createdAt?.toDate() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "vehicles")
                    }
                    throw error
                }
        )
    }

    suspend fun getVehicle(vehicleId: String): Result<Vehicle> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("vehicles").document(vehicleId)
            val snapshot = docRef.get().await()
            val vehicle = snapshot.toObject(Vehicle::class.java)
            if (vehicle != null && vehicle.userId == uid) {
                Result.success(vehicle)
            } else {
                Result.failure(NoSuchElementException("Vehicle not found or unauthorized"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "vehicles/$vehicleId")
            Result.failure(e)
        }
    }

    suspend fun addVehicle(vehicle: Vehicle): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("vehicles").document()
            val payload = vehicle.toCreateMap(uid)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "vehicles")
            Result.failure(e)
        }
    }

    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(vehicle.id.isNotBlank()) { "Vehicle ID must not be blank" }
            val docRef = db.collection("vehicles").document(vehicle.id)
            val payload = vehicle.toUpdateMap()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "vehicles/${vehicle.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteVehicle(vehicleId: String): Result<Unit> {
        return try {
            requireUserId()
            require(vehicleId.isNotBlank()) { "Vehicle ID must not be blank" }
            val docRef = db.collection("vehicles").document(vehicleId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "vehicles/$vehicleId")
            Result.failure(e)
        }
    }

    // --- MAINTENANCE LOGS ---

    fun observeMaintenanceLogs(vehicleId: String? = null): Flow<List<MaintenanceLog>> = flow {
        val uid = requireUserId()
        val baseQuery = db.collection("maintenance_logs").whereEqualTo("userId", uid)
        val query = if (vehicleId != null) {
            baseQuery.whereEqualTo("vehicleId", vehicleId)
        } else {
            baseQuery
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MaintenanceLog::class.java).sortedByDescending { it.serviceDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "maintenance_logs")
                    }
                    throw error
                }
        )
    }

    suspend fun addMaintenanceLog(log: MaintenanceLog): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("maintenance_logs").document()
            val payload = log.toCreateMap(uid)
            docRef.set(payload).await()
            // If log has higher mileage than vehicle's current mileage, update vehicle
            if (log.mileage > 0) {
                try {
                    val vehicleRef = db.collection("vehicles").document(log.vehicleId)
                    val vehicleSnap = vehicleRef.get().await()
                    val currentMileage = vehicleSnap.getLong("currentMileage")?.toInt() ?: 0
                    if (log.mileage > currentMileage) {
                        vehicleRef.update(
                            mapOf(
                                "currentMileage" to log.mileage,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (_: Exception) {
                    // Ignore vehicle mileage auto-update failure
                }
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "maintenance_logs")
            Result.failure(e)
        }
    }

    suspend fun updateMaintenanceLog(log: MaintenanceLog): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(log.id.isNotBlank()) { "Maintenance log ID must not be blank" }
            val docRef = db.collection("maintenance_logs").document(log.id)
            val payload = log.toUpdateMap()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "maintenance_logs/${log.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteMaintenanceLog(logId: String): Result<Unit> {
        return try {
            requireUserId()
            require(logId.isNotBlank()) { "Maintenance log ID must not be blank" }
            val docRef = db.collection("maintenance_logs").document(logId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "maintenance_logs/$logId")
            Result.failure(e)
        }
    }

    // --- SERVICE REMINDERS ---

    fun observeServiceReminders(vehicleId: String? = null): Flow<List<ServiceReminder>> = flow {
        val uid = requireUserId()
        val baseQuery = db.collection("service_reminders").whereEqualTo("userId", uid)
        val query = if (vehicleId != null) {
            baseQuery.whereEqualTo("vehicleId", vehicleId)
        } else {
            baseQuery
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ServiceReminder::class.java).sortedWith(
                        compareBy<ServiceReminder> { it.isCompleted }.thenBy { it.dueDate }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "service_reminders")
                    }
                    throw error
                }
        )
    }

    suspend fun addServiceReminder(reminder: ServiceReminder): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("service_reminders").document()
            val payload = reminder.toCreateMap(uid)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "service_reminders")
            Result.failure(e)
        }
    }

    suspend fun toggleReminderCompletion(reminderId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank" }
            val docRef = db.collection("service_reminders").document(reminderId)
            docRef.update(
                mapOf(
                    "isCompleted" to isCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "service_reminders/$reminderId")
            Result.failure(e)
        }
    }

    suspend fun updateServiceReminder(reminder: ServiceReminder): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(reminder.id.isNotBlank()) { "Reminder ID must not be blank" }
            val docRef = db.collection("service_reminders").document(reminder.id)
            val payload = reminder.toUpdateMap()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "service_reminders/${reminder.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteServiceReminder(reminderId: String): Result<Unit> {
        return try {
            requireUserId()
            require(reminderId.isNotBlank()) { "Reminder ID must not be blank" }
            val docRef = db.collection("service_reminders").document(reminderId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "service_reminders/$reminderId")
            Result.failure(e)
        }
    }
}
