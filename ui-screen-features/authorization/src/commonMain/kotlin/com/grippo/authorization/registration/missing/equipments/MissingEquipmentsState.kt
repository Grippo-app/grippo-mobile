package com.grippo.authorization.registration.missing.equipments

import androidx.compose.runtime.Immutable
import com.grippo.state.equipments.EquipmentGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class MissingEquipmentsState(
    val suggestions: ImmutableList<EquipmentGroupState> = persistentListOf(),
    val selectedGroupId: String? = null,
    val selectedEquipmentIds: PersistentList<String> = persistentListOf()
)