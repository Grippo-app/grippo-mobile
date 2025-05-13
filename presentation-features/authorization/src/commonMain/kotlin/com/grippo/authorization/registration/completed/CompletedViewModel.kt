package com.grippo.authorization.registration.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.domain.mapper.toDomain
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

internal class CompletedViewModel(
    email: String,
    password: String,
    name: String,
    weight: Float,
    height: Int,
    experience: ExperienceEnumState?,
    excludedMuscleIds: ImmutableList<String>,
    missingEquipmentIds: ImmutableList<String>,
    private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<CompletedState, CompletedDirection, CompletedLoader>(
    CompletedState(name = name)
), CompletedContract {

    init {
        safeLaunch(loader = CompletedLoader.Registration) {
            val registration = SetRegistration(
                email = email,
                password = password,
                name = name,
                weight = weight,
                height = height,
                experience = experience?.toDomain() ?: return@safeLaunch,
                excludeEquipmentIds = missingEquipmentIds,
                excludeMuscleIds = excludedMuscleIds
            )

            authorizationFeature.register(registration).getOrThrow()
        }
    }

    override fun complete() {
        navigateTo(CompletedDirection.Home)
    }
}