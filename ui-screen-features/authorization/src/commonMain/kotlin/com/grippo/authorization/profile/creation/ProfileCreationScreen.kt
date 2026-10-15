package com.grippo.authorization.profile.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesContract
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesScreen
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesState
import com.grippo.authorization.profile.creation.experience.ExperienceContract
import com.grippo.authorization.profile.creation.experience.ExperienceScreen
import com.grippo.authorization.profile.creation.experience.ExperienceState
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsContract
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsScreen
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsState
import com.grippo.authorization.profile.creation.user.UserContract
import com.grippo.authorization.profile.creation.user.UserScreen
import com.grippo.authorization.profile.creation.user.UserState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.foundation.platform.platformAnimation
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.components.indicators.StepProgressIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.screen.api.ProfileCreationRouter
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

private val PROFILE_CREATION_FLOW = persistentListOf(
    ProfileCreationRouter.User,
    ProfileCreationRouter.Experience,
    ProfileCreationRouter.ExcludedMuscles,
    ProfileCreationRouter.MissingEquipments,
    ProfileCreationRouter.Completed
)

@Composable
internal fun ProfileCreationScreen(
    component: ProfileCreationComponent,
    state: ProfileCreationState,
    loaders: ImmutableSet<ProfileCreationLoader>,
    contract: ProfileCreationContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val stack by component.childStack.subscribeAsState()
    val stepsTotal = remember { PROFILE_CREATION_FLOW.size }
    val stepsCompleted = stack.items.size.coerceIn(0, stepsTotal)
    val visibleSteps = (stepsTotal - 1).coerceAtLeast(1)

    Box {
        ChildStackCompose(
            modifier = Modifier.fillMaxSize(),
            stack = component.childStack,
            animation = platformAnimation(),
            content = { child -> child.instance.component.Render() }
        )

        if (stepsCompleted < stepsTotal) {
            ProfileCreationProgress(
                modifier = Modifier.align(Alignment.TopCenter),
                stepCount = visibleSteps,
                currentStep = (stepsCompleted - 1).coerceIn(0, visibleSteps - 1),
            )
        }
    }
}

@Composable
private fun ProfileCreationProgress(
    modifier: Modifier = Modifier,
    stepCount: Int,
    currentStep: Int,
) {
    StepProgressIndicator(
        modifier = modifier
            .statusBarsPadding()
            .padding(vertical = AppTokens.dp.contentPadding.content)
            .height(AppTokens.dp.screen.toolbar.height)
            .fillMaxWidth(AppTokens.dp.screen.toolbar.progressWidthFraction)
            .wrapContentHeight(),
        stepCount = stepCount,
        currentStep = currentStep,
    )
}

@AppPreview
@Composable
private fun ProfileCreationUserPreview() {
    PreviewContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            UserScreen(
                state = UserState(
                    name = NameFormatState.of("Alex"),
                    weight = WeightFormatState.of(72.0f),
                    height = HeightFormatState.of(178),
                ),
                loaders = persistentSetOf(),
                contract = UserContract.Empty,
            )
            ProfileCreationProgress(
                modifier = Modifier.align(Alignment.TopCenter),
                stepCount = PROFILE_CREATION_FLOW.size - 1,
                currentStep = 0,
            )
        }
    }
}

@AppPreview
@Composable
private fun ProfileCreationExperiencePreview() {
    PreviewContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            ExperienceScreen(
                state = ExperienceState(selected = ExperienceEnumState.INTERMEDIATE),
                loaders = persistentSetOf(),
                contract = ExperienceContract.Empty,
            )
            ProfileCreationProgress(
                modifier = Modifier.align(Alignment.TopCenter),
                stepCount = PROFILE_CREATION_FLOW.size - 1,
                currentStep = 1,
            )
        }
    }
}

@AppPreview
@Composable
private fun ProfileCreationMusclesPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        Box(modifier = Modifier.fillMaxSize()) {
            ExcludedMusclesScreen(
                state = ExcludedMusclesState(
                    suggestions = groups,
                    selectedGroupId = groups.first().id,
                    selectedMuscleIds = persistentListOf(groups.first().muscles.first().value.id),
                ),
                loaders = persistentSetOf(),
                contract = ExcludedMusclesContract.Empty,
            )
            ProfileCreationProgress(
                modifier = Modifier.align(Alignment.TopCenter),
                stepCount = PROFILE_CREATION_FLOW.size - 1,
                currentStep = 2,
            )
        }
    }
}

@AppPreview
@Composable
private fun ProfileCreationEquipmentsPreview() {
    PreviewContainer {
        val groups = stubEquipments()
        Box(modifier = Modifier.fillMaxSize()) {
            MissingEquipmentsScreen(
                state = MissingEquipmentsState(
                    suggestions = groups,
                    selectedGroupId = groups.first().id,
                    selectedEquipmentIds = persistentListOf(groups.first().equipments.first().id),
                ),
                loaders = persistentSetOf(),
                contract = MissingEquipmentsContract.Empty,
            )
            ProfileCreationProgress(
                modifier = Modifier.align(Alignment.TopCenter),
                stepCount = PROFILE_CREATION_FLOW.size - 1,
                currentStep = 3,
            )
        }
    }
}
