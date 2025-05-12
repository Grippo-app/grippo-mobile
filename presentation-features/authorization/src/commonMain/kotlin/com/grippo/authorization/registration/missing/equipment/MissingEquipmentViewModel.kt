package com.grippo.authorization.registration.missing.equipment

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.domain.mapper.toDomain
import com.grippo.domain.mapper.toState
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class MissingEquipmentViewModel(
    private val email: String,
    private val password: String,
    private val name: String,
    private val weight: Float,
    private val height: Int,
    private val experience: ExperienceEnumState?,
    private val excludedMuscleIds: ImmutableList<String>,
    private val equipmentFeature: EquipmentFeature,
    private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<MissingEquipmentState, MissingEquipmentDirection, MissingEquipmentLoader>(
    MissingEquipmentState()
), MissingEquipmentContract {

    init {
        equipmentFeature
            .observeEquipments()
            .onEach(::provideEquipments)
            .catch { sendError(it) }
            .launchIn(coroutineScope)

        safeLaunch {
            equipmentFeature.getPublicEquipments().getOrThrow()
        }
    }

    private fun provideEquipments(list: List<EquipmentGroup>) {
        val suggestions = list.toState()
        val selectedIds = suggestions.flatMap { it.equipments }.map { it.id }.toPersistentList()
        update {
            it.copy(
                suggestions = suggestions,
                selectedEquipmentIds = selectedIds,
                selectedGroupId = suggestions.firstOrNull()?.id
            )
        }
    }

    override fun selectGroup(id: String) {
        update { it.copy(selectedGroupId = id) }
    }

    override fun selectEquipment(id: String) {
        update {
            val newList: PersistentList<String> = it.selectedEquipmentIds
                .toMutableList()
                .apply { if (contains(id)) remove(id) else add(id) }
                .toPersistentList()

            it.copy(selectedEquipmentIds = newList)
        }
    }

    override fun next() {
        safeLaunch {
            val formattedMissingEquipments = state.value.suggestions
                .map { it.id } - state.value.selectedEquipmentIds

            val formattedExperience = experience
                ?.toDomain() ?: return@safeLaunch

            val registration = SetRegistration(
                email = email,
                password = password,
                name = name,
                weight = weight,
                height = height,
                experience = formattedExperience,
                excludeEquipmentIds = formattedMissingEquipments,
                excludeMuscleIds = excludedMuscleIds
            )
            authorizationFeature.register(registration)
        }

        val direction = MissingEquipmentDirection.Completed
        navigateTo(direction)
    }
}