package com.grippo.authorization.registration.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.RegisterUseCase
import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.mapper.user.toDomain
import com.grippo.domain.mapper.user.toState
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.onEach

internal class CompletedViewModel(
    email: String,
    password: String,
    name: String,
    weight: Float,
    height: Int,
    experience: ExperienceEnumState?,
    excludedMuscleIds: ImmutableList<String>,
    missingEquipmentIds: ImmutableList<String>,
    private val registerUseCase: RegisterUseCase,
    private val userFeature: UserFeature,
) : BaseViewModel<CompletedState, CompletedDirection, CompletedLoader>(
    CompletedState()
), CompletedContract {

    init {
        userFeature
            .observeUser()
            .onEach(::provideUser)
            .safeLaunch()

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

            registerUseCase.execute(registration)
        }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState()
        update { it.copy(user = user) }
    }

    override fun complete() {
        navigateTo(CompletedDirection.Home)
    }
}