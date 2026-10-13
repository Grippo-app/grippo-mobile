package com.grippo.authorization.profile.creation.completed

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.user.CreateProfileUseCase
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.state.domain.user.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.ImmutableList

internal class ProfileCompletedViewModel(
    name: String,
    weight: Float,
    height: Int,
    experience: ExperienceEnumState?,
    excludedMuscleIds: ImmutableList<String>,
    missingEquipmentIds: ImmutableList<String>,
    private val analytics: AuthAnalytics,
    createProfileUseCase: CreateProfileUseCase,
    localSettingsFeature: LocalSettingsFeature,
) : BaseViewModel<ProfileCompletedState, ProfileCompletedDirection, ProfileCompletedLoader>(
    ProfileCompletedState
), ProfileCompletedContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Completed, reason)
    }

    init {
        val mappedExperience = experience?.toDomain()

        if (mappedExperience == null) {
            navigateTo(ProfileCompletedDirection.Back)
        } else {
            safeLaunch(
                loader = ProfileCompletedLoader.ProfileCreation,
                onError = {
                    navigateTo(ProfileCompletedDirection.Back)
                }
            ) {
                analytics.trackOperation(AuthOperation.CreateProfile) {
                    val profile = CreateUserProfile(
                        name = name,
                        weight = weight,
                        height = height,
                        experience = mappedExperience,
                        excludeEquipmentIds = missingEquipmentIds.toList(),
                        excludeMuscleIds = excludedMuscleIds.toList()
                    )

                    createProfileUseCase.execute(profile)

                    localSettingsFeature
                        .setHomeWelcomeStatus(HomeWelcomeStatus.PendingCelebration)
                        .getOrThrow()
                }

                navigateTo(ProfileCompletedDirection.Home)
            }
        }
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Completed.Back)
        navigateTo(ProfileCompletedDirection.Back)
    }
}
