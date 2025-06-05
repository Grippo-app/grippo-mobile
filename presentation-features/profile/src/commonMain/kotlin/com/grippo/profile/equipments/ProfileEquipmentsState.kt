package com.grippo.profile.equipments

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.equipment.models.EquipmentGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ProfileEquipmentsState(
    val suggestions: ImmutableList<EquipmentGroupState> = persistentListOf(),
    val selectedGroupId: String? = null,
    val selectedEquipmentIds: PersistentList<String> = persistentListOf()
)