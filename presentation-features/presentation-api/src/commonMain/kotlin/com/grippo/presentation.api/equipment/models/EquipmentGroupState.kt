package com.grippo.presentation.api.equipment.models

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList


@Immutable
public data class EquipmentGroupState(
    val id: String,
    val name: String,
    val equipments: ImmutableList<EquipmentState>
)