package com.grippo.authorization.profile.creation.completed

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.user.CreateProfileUseCase
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.services.firebase.FirebaseProvider.Event
import com.grippo.state.domain.user.toDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.onEach

internal class ProfileCompletedViewModel(
    name: String,
    weight: Float,
    height: Int,
    experience: ExperienceEnumState?,
    excludedMuscleIds: ImmutableList<String>,
    missingEquipmentIds: ImmutableList<String>,
    userFeature: UserFeature,
    private val createProfileUseCase: CreateProfileUseCase,
) : BaseViewModel<ProfileCompletedState, ProfileCompletedDirection, ProfileCompletedLoader>(
    ProfileCompletedState()
), ProfileCompletedContract {

    init {
        FirebaseProvider.logEvent(Event.REGISTRATION_COMPLETED)

        userFeature
            .observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        safeLaunch(loader = ProfileCompletedLoader.ProfileCreation) {
            val mappedExperience = experience?.toDomain() ?: return@safeLaunch

            val profile = CreateUserProfile(
                name = name,
                weight = weight,
                height = height,
                experience = mappedExperience,
                excludeEquipmentIds = missingEquipmentIds.toList(),
                excludeMuscleIds = excludedMuscleIds.toList()
            )

            createProfileUseCase.execute(profile)
        }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState()
        update { it.copy(user = user) }
    }

    override fun onCompleteClick() {
        navigateTo(ProfileCompletedDirection.Home)
    }

    override fun onBack() {
        navigateTo(ProfileCompletedDirection.Back)
    }
}
