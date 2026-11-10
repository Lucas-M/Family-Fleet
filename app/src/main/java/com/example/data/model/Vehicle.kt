package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val familyMember: String = "",
    val nickname: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 2024,
    val vin: String = "",
    val licensePlate: String = "",
    val state: String = "",
    val currentMileage: Int = 0,
    val insuranceCompany: String = "",
    val insurancePolicyNumber: String = "",
    val insuranceDuration: String = "",
    val insuranceStartDate: String = "",
    val insuranceEndDate: String = "",
    val colorHex: String = "#2563EB",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "familyMember" to familyMember.trim(),
            "make" to make.trim(),
            "model" to model.trim(),
            "year" to year,
            "vin" to vin.trim().uppercase(),
            "licensePlate" to licensePlate.trim().uppercase(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (nickname.isNotBlank()) map["nickname"] = nickname.trim()
        if (state.isNotBlank()) map["state"] = state.trim().uppercase()
        if (currentMileage >= 0) map["currentMileage"] = currentMileage
        if (insuranceCompany.isNotBlank()) map["insuranceCompany"] = insuranceCompany.trim()
        if (insurancePolicyNumber.isNotBlank()) map["insurancePolicyNumber"] = insurancePolicyNumber.trim()
        if (insuranceDuration.isNotBlank()) map["insuranceDuration"] = insuranceDuration.trim()
        if (insuranceStartDate.isNotBlank()) map["insuranceStartDate"] = insuranceStartDate.trim()
        if (insuranceEndDate.isNotBlank()) map["insuranceEndDate"] = insuranceEndDate.trim()
        if (colorHex.isNotBlank()) map["colorHex"] = colorHex.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }

    fun toUpdateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "familyMember" to familyMember.trim(),
            "make" to make.trim(),
            "model" to model.trim(),
            "year" to year,
            "vin" to vin.trim().uppercase(),
            "licensePlate" to licensePlate.trim().uppercase(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (nickname.isNotBlank()) map["nickname"] = nickname.trim()
        if (state.isNotBlank()) map["state"] = state.trim().uppercase()
        if (currentMileage >= 0) map["currentMileage"] = currentMileage
        if (insuranceCompany.isNotBlank()) map["insuranceCompany"] = insuranceCompany.trim()
        if (insurancePolicyNumber.isNotBlank()) map["insurancePolicyNumber"] = insurancePolicyNumber.trim()
        if (insuranceDuration.isNotBlank()) map["insuranceDuration"] = insuranceDuration.trim()
        if (insuranceStartDate.isNotBlank()) map["insuranceStartDate"] = insuranceStartDate.trim()
        if (insuranceEndDate.isNotBlank()) map["insuranceEndDate"] = insuranceEndDate.trim()
        if (colorHex.isNotBlank()) map["colorHex"] = colorHex.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map.filterValues { it != null }
    }
}
