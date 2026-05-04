package com.grippo.authorization.profile.creation.completed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.onboarding.OnboardingChecklist
import com.grippo.design.components.onboarding.OnboardingChecklistItem
import com.grippo.design.components.onboarding.OnboardingExercisePackCard
import com.grippo.design.components.onboarding.OnboardingProfileFacts
import com.grippo.design.components.onboarding.OnboardingProgressBadge
import com.grippo.design.components.onboarding.OnboardingWelcomeBlock
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.UserCard
import com.grippo.design.components.user.UserCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.get_started_btn
import com.grippo.design.resources.provider.onboarding_check_equipment
import com.grippo.design.resources.provider.onboarding_check_experience
import com.grippo.design.resources.provider.onboarding_check_muscles
import com.grippo.design.resources.provider.onboarding_check_profile
import com.grippo.design.resources.provider.onboarding_checklist_title
import com.grippo.design.resources.provider.registration_completed_subtitle
import com.grippo.design.resources.provider.registration_completed_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileCompletedScreen(
    state: ProfileCompletedState,
    loaders: ImmutableSet<ProfileCompletedLoader>,
    contract: ProfileCompletedContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {

    if (loaders.contains(ProfileCompletedLoader.ProfileCreation)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    val contentVisible = remember { mutableStateOf(false) }

    LaunchedEffect(state.user, loaders) {
        val hasLoader = loaders.contains(ProfileCompletedLoader.ProfileCreation)
        val hasUser = state.user != null
        contentVisible.value = hasUser && hasLoader.not()
    }

    val alpha by animateFloatAsState(
        targetValue = if (contentVisible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "content-alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (contentVisible.value) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "content-offsetY"
    )

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .imePadding()
    ) {
        val basePadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.screen.verticalPadding,
        )

        BottomOverlayContainer(
            modifier = Modifier.fillMaxSize(),
            contentPadding = basePadding,
            overlay = AppTokens.colors.background.screen,
            content = { containerModifier, resolvedPadding ->
                if (state.user != null) {
                    ProfileCompletedContent(
                        modifier = containerModifier
                            .fillMaxSize()
                            .offset(y = offsetY)
                            .alpha(alpha),
                        contentPadding = resolvedPadding,
                        user = state.user,
                        experience = state.experience ?: state.user.experience,
                        excludedMusclesCount = state.excludedMusclesCount,
                        missingEquipmentCount = state.missingEquipmentCount,
                    )
                }
            },
            bottom = {
                Button(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                        .fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.get_started_btn),
                    ),
                    style = ButtonStyle.Primary,
                    onClick = contract::onCompleteClick
                )

                Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

                Spacer(Modifier.navigationBarsPadding())
            }
        )

        if (state.user != null && contentVisible.value) {
            KonfettiParade()
        }
    }
}

@Composable
private fun ProfileCompletedContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    user: UserState,
    experience: ExperienceEnumState,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
) {
    val checklistItems = persistentListOf(
        OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_profile)),
        OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_experience)),
        OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_muscles)),
        OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_equipment)),
    )

    val checklistTitle = AppTokens.strings.res(Res.string.onboarding_checklist_title)

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        item("hero") {
            HeroBlock(
                modifier = Modifier.fillMaxWidth()
            )
        }

        item("progress") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingProgressBadge()
            }
        }

        item("user-card") {
            UserCard(
                modifier = Modifier.fillMaxWidth(),
                value = user,
                style = UserCardStyle.Detailed,
            )
        }

        item("profile-facts") {
            OnboardingProfileFacts(
                modifier = Modifier.fillMaxWidth(),
                heightDisplay = user.height.display,
                weightDisplay = user.weight.display,
                excludedMusclesCount = excludedMusclesCount,
                missingEquipmentCount = missingEquipmentCount,
            )
        }

        item("welcome") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingWelcomeBlock(
                    modifier = Modifier.fillMaxWidth(),
                    experience = experience,
                )
            }
        }

        item("checklist") {
            OnboardingChecklist(
                modifier = Modifier.fillMaxWidth(),
                title = checklistTitle,
                items = checklistItems,
            )
        }

        item("exercise-pack") {
            OnboardingExercisePackCard(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroBlock(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_completed_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_completed_subtitle),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileCompletedScreen(
            state = ProfileCompletedState(
                user = stubUser(),
                experience = ExperienceEnumState.PRO,
                excludedMusclesCount = 3,
                missingEquipmentCount = 5,
            ),
            loaders = persistentSetOf(),
            contract = ProfileCompletedContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewBeginnerAllAvailable() {
    PreviewContainer {
        ProfileCompletedScreen(
            state = ProfileCompletedState(
                user = stubUser().copy(experience = ExperienceEnumState.BEGINNER),
                experience = ExperienceEnumState.BEGINNER,
                excludedMusclesCount = 0,
                missingEquipmentCount = 0,
            ),
            loaders = persistentSetOf(),
            contract = ProfileCompletedContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        ProfileCompletedScreen(
            state = ProfileCompletedState(
                user = stubUser()
            ),
            loaders = persistentSetOf(ProfileCompletedLoader.ProfileCreation),
            contract = ProfileCompletedContract.Empty
        )
    }
}
