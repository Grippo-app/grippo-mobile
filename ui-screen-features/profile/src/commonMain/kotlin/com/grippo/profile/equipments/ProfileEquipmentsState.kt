package com.grippo.profile.equipments

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.equipments.EquipmentGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class ProfileEquipmentsState(
    @Transient
    val suggestions: ImmutableList<EquipmentGroupState> = persistentListOf(),
    val selectedGroupId: String? = null,
    @Serializable(with = ImmutableListSerializer::class)
    val selectedEquipmentIds: ImmutableList<String> = persistentListOf(),
    @Serializable(with = ImmutableListSerializer::class)
    val knownEquipmentIds: ImmutableList<String> = persistentListOf(),
)
