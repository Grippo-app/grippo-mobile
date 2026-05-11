package com.grippo.home.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.user.UserCard
import com.grippo.design.components.user.UserCardStyle
import com.grippo.design.components.welcome.WelcomeBlock
import com.grippo.design.components.welcome.WelcomeChecklist
import com.grippo.design.components.welcome.WelcomeChecklistItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_title
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Lock
import com.grippo.design.resources.provider.icons.Medal
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.icons.Stack
import com.grippo.design.resources.provider.resume_training_btn
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.welcome_benefit_history_subtitle
import com.grippo.design.resources.provider.welcome_benefit_history_title
import com.grippo.design.resources.provider.welcome_benefit_pack_subtitle
import com.grippo.design.resources.provider.welcome_benefit_pack_title
import com.grippo.design.resources.provider.welcome_benefit_progress_subtitle
import com.grippo.design.resources.provider.welcome_benefit_progress_title
import com.grippo.design.resources.provider.welcome_check_equipment
import com.grippo.design.resources.provider.welcome_check_experience
import com.grippo.design.resources.provider.welcome_check_goal
import com.grippo.design.resources.provider.welcome_check_muscles
import com.grippo.design.resources.provider.welcome_check_profile
import com.grippo.design.resources.provider.welcome_section_benefits_title
import com.grippo.design.resources.provider.welcome_section_progress_title
import com.grippo.design.resources.provider.welcome_subtitle
import com.grippo.design.resources.provider.welcome_title
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WelcomeHomeContent(
    modifier: Modifier,
    user: UserState,
    experience: ExperienceEnumState,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
    hasGoal: Boolean,
    hasDraftTraining: Boolean,
    showWelcomeConfetti: Boolean,
    onStartTraining: () -> Unit,
    onResumeTraining: () -> Unit,
) {
    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.screen.verticalPadding,
    )

    Box(modifier = modifier) {
        BottomOverlayContainer(
            modifier = Modifier.fillMaxSize(),
            contentPadding = basePadding,
            overlay = AppTokens.colors.background.screen,
            content = { containerModifier, resolvedPadding ->
                WelcomeBody(
                    modifier = containerModifier.fillMaxSize(),
                    contentPadding = resolvedPadding,
                    user = user,
                    experience = experience,
                    excludedMusclesCount = excludedMusclesCount,
                    missingEquipmentCount = missingEquipmentCount,
                    hasGoal = hasGoal,
                )
            },
            bottom = {
                if (hasDraftTraining) {
                    Button(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth(),
                        onClick = onResumeTraining,
                        style = ButtonStyle.Error,
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.resume_training_btn)
                        )
                    )
                } else {
                    Button(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.start_workout),
                        ),
                        style = ButtonStyle.Primary,
                        onClick = onStartTraining
                    )
                }

                Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

                Spacer(Modifier.navigationBarsPadding())
            }
        )

        if (showWelcomeConfetti) {
            KonfettiParade()
        }
    }
}

@Composable
private fun WelcomeBody(
    modifier: Modifier,
    contentPadding: PaddingValues,
    user: UserState,
    experience: ExperienceEnumState,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
    hasGoal: Boolean,
) {
    val checklistItems = persistentListOf(
        WelcomeChecklistItem(
            AppTokens.strings.res(Res.string.welcome_check_profile),
            isCompleted = true
        ),
        WelcomeChecklistItem(
            AppTokens.strings.res(Res.string.welcome_check_experience),
            isCompleted = true
        ),
        WelcomeChecklistItem(
            AppTokens.strings.res(Res.string.welcome_check_muscles),
            isCompleted = true
        ),
        WelcomeChecklistItem(
            AppTokens.strings.res(Res.string.welcome_check_equipment),
            isCompleted = true
        ),
        WelcomeChecklistItem(
            text = AppTokens.strings.res(Res.string.welcome_check_goal),
            isCompleted = hasGoal
        ),
    )

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

        item("user-card") {
            UserCard(
                modifier = Modifier.fillMaxWidth(),
                value = user,
                style = UserCardStyle.Detailed,
            )
        }

        item("welcome-block") {
            WelcomeBlock(
                modifier = Modifier.fillMaxWidth(),
                experience = experience,
                heightDisplay = user.height.display,
                weightDisplay = user.weight.display,
                excludedMusclesCount = excludedMusclesCount,
                missingEquipmentCount = missingEquipmentCount,
            )
        }

        item("progress-header") {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.welcome_section_progress_title),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )
        }

        item("checklist") {
            WelcomeChecklist(
                modifier = Modifier.fillMaxWidth(),
                items = checklistItems,
            )
        }

        item("benefits-header") {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.welcome_section_benefits_title),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )
        }

        item("benefits") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                BannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = BannerCardStyle.Custom(AppTokens.colors.brand.color6),
                    icon = AppTokens.icons.Sparkle,
                    title = AppTokens.strings.res(Res.string.welcome_benefit_pack_title),
                    description = AppTokens.strings.res(Res.string.welcome_benefit_pack_subtitle),
                )

                BannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = BannerCardStyle.Custom(AppTokens.colors.brand.color6),
                    icon = AppTokens.icons.LineUp,
                    title = AppTokens.strings.res(Res.string.welcome_benefit_progress_title),
                    description = AppTokens.strings.res(Res.string.welcome_benefit_progress_subtitle),
                )

                BannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = BannerCardStyle.Custom(AppTokens.colors.brand.color6),
                    icon = AppTokens.icons.Stack,
                    title = AppTokens.strings.res(Res.string.welcome_benefit_history_title),
                    description = AppTokens.strings.res(Res.string.welcome_benefit_history_subtitle),
                )

                BannerCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = BannerCardStyle.Custom(AppTokens.colors.brand.color6),
                    icon = if (hasGoal) AppTokens.icons.Medal else AppTokens.icons.Lock,
                    title = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title),
                    description = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description),
                    enabled = hasGoal,
                )
            }
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
            text = AppTokens.strings.res(Res.string.welcome_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.welcome_subtitle),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentPreview() {
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = stubUser(),
            experience = ExperienceEnumState.PRO,
            excludedMusclesCount = 3,
            missingEquipmentCount = 5,
            hasGoal = true,
            hasDraftTraining = false,
            showWelcomeConfetti = false,
            onStartTraining = {},
            onResumeTraining = {},
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentDraftPreview() {
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = stubUser().copy(experience = ExperienceEnumState.BEGINNER),
            experience = ExperienceEnumState.BEGINNER,
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
            hasGoal = false,
            hasDraftTraining = true,
            showWelcomeConfetti = false,
            onStartTraining = {},
            onResumeTraining = {},
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentCelebrationPreview() {
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = stubUser(),
            experience = ExperienceEnumState.BEGINNER,
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
            hasGoal = false,
            hasDraftTraining = false,
            showWelcomeConfetti = true,
            onStartTraining = {},
            onResumeTraining = {},
        )
    }
}
