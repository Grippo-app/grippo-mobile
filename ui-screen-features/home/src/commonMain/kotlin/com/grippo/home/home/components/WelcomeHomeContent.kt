package com.grippo.home.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.modifiers.IllustrationBackgroundStyle
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.welcome.WelcomeBlock
import com.grippo.design.components.welcome.WelcomeChecklist
import com.grippo.design.components.welcome.WelcomeChecklistItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_workouts
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_title
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Compass
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.icons.Stack
import com.grippo.design.resources.provider.welcome_benefit_history_subtitle
import com.grippo.design.resources.provider.welcome_benefit_history_title
import com.grippo.design.resources.provider.welcome_benefit_pack_subtitle
import com.grippo.design.resources.provider.welcome_benefit_pack_title
import com.grippo.design.resources.provider.welcome_benefit_progress_subtitle
import com.grippo.design.resources.provider.welcome_benefit_progress_title
import com.grippo.design.resources.provider.welcome_check_equipment
import com.grippo.design.resources.provider.welcome_check_experience
import com.grippo.design.resources.provider.welcome_check_goal
import com.grippo.design.resources.provider.welcome_check_goal_pending
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
    showWelcomeConfetti: Boolean,
    onWelcomeConfettiShown: () -> Unit,
) {
    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.screen.verticalPadding,
    )

    Box(modifier = modifier) {
        WelcomeBody(
            modifier = Modifier.fillMaxSize(),
            contentPadding = basePadding,
            user = user,
            experience = experience,
            excludedMusclesCount = excludedMusclesCount,
            missingEquipmentCount = missingEquipmentCount,
            hasGoal = hasGoal,
        )

        KonfettiParade(
            show = showWelcomeConfetti,
            onShown = onWelcomeConfettiShown,
        )
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
            text = AppTokens.strings.res(
                if (hasGoal) Res.string.welcome_check_goal else Res.string.welcome_check_goal_pending,
            ),
            isCompleted = hasGoal
        ),
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        item("hero") {
            HeroBlock(
                modifier = Modifier.fillMaxWidth(),
                user = user,
            )
        }

        item("progress") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.welcome_section_progress_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
                WelcomeChecklist(
                    modifier = Modifier.fillMaxWidth(),
                    items = checklistItems,
                )
            }
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

        item("benefits") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.welcome_section_benefits_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
                ) {
                    BannerCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = BannerCardStyle.Custom(
                            tint = AppTokens.colors.icon.secondary,
                            surface = AppTokens.colors.bannerCard.mutedBackground,
                        ),
                        icon = AppTokens.icons.Sparkle,
                        title = AppTokens.strings.res(Res.string.welcome_benefit_pack_title),
                        description = AppTokens.strings.res(Res.string.welcome_benefit_pack_subtitle),
                    )

                    BannerCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = BannerCardStyle.Custom(
                            tint = AppTokens.colors.icon.secondary,
                            surface = AppTokens.colors.bannerCard.mutedBackground,
                        ),
                        icon = AppTokens.icons.LineUp,
                        title = AppTokens.strings.res(Res.string.welcome_benefit_progress_title),
                        description = AppTokens.strings.res(Res.string.welcome_benefit_progress_subtitle),
                    )

                    BannerCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = BannerCardStyle.Custom(
                            tint = AppTokens.colors.icon.secondary,
                            surface = AppTokens.colors.bannerCard.mutedBackground,
                        ),
                        icon = AppTokens.icons.Stack,
                        title = AppTokens.strings.res(Res.string.welcome_benefit_history_title),
                        description = AppTokens.strings.res(Res.string.welcome_benefit_history_subtitle),
                    )

                    BannerCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = BannerCardStyle.Custom(
                            tint = AppTokens.colors.icon.secondary,
                            surface = AppTokens.colors.bannerCard.mutedBackground,
                        ),
                        icon = AppTokens.icons.Compass,
                        title = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title),
                        description = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description),
                        enabled = hasGoal,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBlock(
    modifier: Modifier = Modifier,
    user: UserState,
) {
    val shape = RoundedCornerShape(AppTokens.dp.welcome.card.radius)
    Column(
        modifier = modifier
            .illustrationBackground(shape, style = IllustrationBackgroundStyle.Solid)
            .padding(AppTokens.dp.contentPadding.block),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = user.name,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.secondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.welcome_title),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary,
            )

            Illustration(
                modifier = Modifier.width(AppTokens.dp.illustration.compact),
                painter = AppTokens.drawables.res(Res.drawable.empty_workouts),
                style = IllustrationStyle.Compact,
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.welcome_subtitle),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentWithGoalPreview() {
    val user = requireNotNull(homePreviewState(lifetimeTrainingCount = 0).user)
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = user.copy(experience = ExperienceEnumState.PRO),
            experience = ExperienceEnumState.PRO,
            excludedMusclesCount = 3,
            missingEquipmentCount = 5,
            hasGoal = true,
            showWelcomeConfetti = false,
            onWelcomeConfettiShown = {},
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentWithoutGoalPreview() {
    val user = requireNotNull(homePreviewState(lifetimeTrainingCount = 0, hasGoal = false).user)
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = user.copy(experience = ExperienceEnumState.BEGINNER),
            experience = ExperienceEnumState.BEGINNER,
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
            hasGoal = false,
            showWelcomeConfetti = false,
            onWelcomeConfettiShown = {},
        )
    }
}

@AppPreview
@Composable
private fun WelcomeHomeContentCelebrationPreview() {
    val user = requireNotNull(homePreviewState(lifetimeTrainingCount = 0, hasGoal = false).user)
    PreviewContainer {
        WelcomeHomeContent(
            modifier = Modifier.fillMaxSize(),
            user = user.copy(experience = ExperienceEnumState.BEGINNER),
            experience = ExperienceEnumState.BEGINNER,
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
            hasGoal = false,
            showWelcomeConfetti = true,
            onWelcomeConfettiShown = {},
        )
    }
}
