package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MaintenanceLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val title: String = "",
    val logType: String = "MAINTENANCE", // MAINTENANCE, REPAIR, INSPECTION
    val serviceDate: String = "",
    val mileage: Int = 0,
    val cost: Double = 0.0,
    val serviceProvider: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "vehicleId" to vehicleId,
            "title" to title.trim(),
            "logType" to logType,
            "serviceDate" to serviceDate.trim(),
            "mileage" to mileage,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (cost >= 0) map["cost"] = cost
        if (serviceProvider.isNotBlank()) map["serviceProvider"] = serviceProvider.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }

    fun toUpdateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "title" to title.trim(),
            "logType" to logType,
            "serviceDate" to serviceDate.trim(),
            "mileage" to mileage,
            "cost" to cost,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (serviceProvider.isNotBlank()) map["serviceProvider"] = serviceProvider.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }
}
