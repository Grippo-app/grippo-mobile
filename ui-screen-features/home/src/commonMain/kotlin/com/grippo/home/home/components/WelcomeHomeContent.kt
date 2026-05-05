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
import com.grippo.design.components.benefit.BenefitCard
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.user.UserCard
import com.grippo.design.components.user.UserCardStyle
import com.grippo.design.components.welcome.WelcomeBlock
import com.grippo.design.components.welcome.WelcomeChecklist
import com.grippo.design.components.welcome.WelcomeChecklistItem
import com.grippo.design.components.welcome.WelcomeProfileFacts
import com.grippo.design.components.welcome.WelcomeProgressBadge
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.LineUp
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
    hasDraftTraining: Boolean,
    onStartTraining: () -> Unit,
    onResumeTraining: () -> Unit,
) {
    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.screen.verticalPadding,
    )

    BottomOverlayContainer(
        modifier = modifier,
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
}

@Composable
private fun WelcomeBody(
    modifier: Modifier,
    contentPadding: PaddingValues,
    user: UserState,
    experience: ExperienceEnumState,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
) {
    val checklistItems = persistentListOf(
        WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_profile)),
        WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_experience)),
        WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_muscles)),
        WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_equipment)),
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

        item("progress") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                WelcomeProgressBadge()
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
            WelcomeProfileFacts(
                modifier = Modifier.fillMaxWidth(),
                heightDisplay = user.height.display,
                weightDisplay = user.weight.display,
                excludedMusclesCount = excludedMusclesCount,
                missingEquipmentCount = missingEquipmentCount,
            )
        }

        item("welcome-block") {
            WelcomeBlock(
                modifier = Modifier.fillMaxWidth(),
                experience = experience,
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
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                BenefitCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = AppTokens.strings.res(Res.string.welcome_benefit_pack_title),
                    subtitle = AppTokens.strings.res(Res.string.welcome_benefit_pack_subtitle),
                    icon = AppTokens.icons.Sparkle,
                    tint = AppTokens.colors.brand.color2,
                )

                BenefitCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = AppTokens.strings.res(Res.string.welcome_benefit_progress_title),
                    subtitle = AppTokens.strings.res(Res.string.welcome_benefit_progress_subtitle),
                    icon = AppTokens.icons.LineUp,
                    tint = AppTokens.colors.brand.color2,
                )

                BenefitCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = AppTokens.strings.res(Res.string.welcome_benefit_history_title),
                    subtitle = AppTokens.strings.res(Res.string.welcome_benefit_history_subtitle),
                    icon = AppTokens.icons.Stack,
                    tint = AppTokens.colors.brand.color2,
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
            hasDraftTraining = false,
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
            hasDraftTraining = true,
            onStartTraining = {},
            onResumeTraining = {},
        )
    }
}
