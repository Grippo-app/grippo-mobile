package com.grippo.authorization.profile.creation.completed

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.user.CreateProfileUseCase
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.services.firebase.FirebaseProvider.Event
import com.grippo.state.domain.user.toDomain
import kotlinx.collections.immutable.ImmutableList

internal class ProfileCompletedViewModel(
    name: String,
    weight: Float,
    height: Int,
    experience: ExperienceEnumState?,
    excludedMuscleIds: ImmutableList<String>,
    missingEquipmentIds: ImmutableList<String>,
    private val createProfileUseCase: CreateProfileUseCase,
) : BaseViewModel<ProfileCompletedState, ProfileCompletedDirection, ProfileCompletedLoader>(
    ProfileCompletedState
), ProfileCompletedContract {

    init {
        FirebaseProvider.logEvent(Event.REGISTRATION_COMPLETED)

        val mappedExperience = experience?.toDomain()

        if (mappedExperience == null) {
            navigateTo(ProfileCompletedDirection.Back)
        } else {
            safeLaunch(
                loader = ProfileCompletedLoader.ProfileCreation,
                onError = ::onBack
            ) {
                val profile = CreateUserProfile(
                    name = name,
                    weight = weight,
                    height = height,
                    experience = mappedExperience,
                    excludeEquipmentIds = missingEquipmentIds.toList(),
                    excludeMuscleIds = excludedMuscleIds.toList()
                )

                createProfileUseCase.execute(profile)

                navigateTo(ProfileCompletedDirection.Home)
            }
        }
    }

    override fun onBack() {
        navigateTo(ProfileCompletedDirection.Back)
    }
}
