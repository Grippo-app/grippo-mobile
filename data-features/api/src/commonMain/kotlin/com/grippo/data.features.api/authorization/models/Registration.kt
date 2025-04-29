package com.grippo.data.features.api.authorization.models

public data class Registration(
    val email: String,
    val password: String,
    val name: String,
    val weight: Double,
    val experience: String,
    val height: Double,
    val excludeMuscleIds: List<String>,
    val excludeEquipmentIds: List<String>
)