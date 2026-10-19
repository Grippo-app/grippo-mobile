package com.grippo.core.state.equipments

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class EquipmentState(
    val id: String,
    val type: EquipmentEnumState,
)
