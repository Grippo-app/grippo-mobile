package com.grippo.authorization.registration.missing.equipment

import androidx.compose.runtime.Immutable
import com.grippo.network.dto.ExcludedEquipmentDto
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class MissingEquipmentState(
    val suggestions: ImmutableList<ExcludedEquipmentDto> = persistentListOf(),
    val selectedEquipmentIds: PersistentList<String> = persistentListOf()
)