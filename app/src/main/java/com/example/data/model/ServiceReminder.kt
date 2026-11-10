package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ServiceReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val serviceType: String = "",
    val dueDate: String = "",
    val dueMileage: Int = 0,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "vehicleId" to vehicleId,
            "serviceType" to serviceType.trim(),
            "isCompleted" to isCompleted,
            "priority" to priority,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (dueDate.isNotBlank()) map["dueDate"] = dueDate.trim()
        if (dueMileage > 0) map["dueMileage"] = dueMileage
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }

    fun toUpdateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "serviceType" to serviceType.trim(),
            "isCompleted" to isCompleted,
            "priority" to priority,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (dueDate.isNotBlank()) map["dueDate"] = dueDate.trim()
        if (dueMileage > 0) map["dueMileage"] = dueMileage
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }
}
