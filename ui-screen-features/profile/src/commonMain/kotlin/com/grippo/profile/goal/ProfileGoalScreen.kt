package com.grippo.profile.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.inputs.InputDate
import com.grippo.design.components.inputs.InputPrimaryGoal
import com.grippo.design.components.inputs.InputSecondaryGoal
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_intro
import com.grippo.design.resources.provider.goal_main_section
import com.grippo.design.resources.provider.goal_save_btn
import com.grippo.design.resources.provider.goal_target_date_placeholder
import com.grippo.design.resources.provider.goal_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

@Composable
internal fun ProfileGoalScreen(
    state: ProfileGoalState,
    loaders: ImmutableSet<ProfileGoalLoader>,
    contract: ProfileGoalContract,
) = BaseComposeScreen(
    ScreenBackground.Color(AppTokens.colors.background.screen)
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.goal_title),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.goal_intro),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
    )

    val groupedPersonalization: Map<PersonalizationKeyEnumState.Category, ImmutableList<PersonalizationKeyEnumState>> =
        remember(state.personalization) {
            state.personalization
                .groupBy(PersonalizationKeyEnumState::category)
                .mapValues { (_, items) -> items.toPersistentList() }
        }

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
                contentPadding = resolvedPadding,
            ) {
                item(key = "main_goal_content") {
                    MainGoalSection(
                        modifier = Modifier.fillMaxWidth(),
                        primary = state.selectedPrimary,
                        secondary = state.selectedSecondary,
                        target = state.selectedTarget,
                        onPrimaryClick = contract::onPrimaryGoalPickerClick,
                        onSecondaryClick = contract::onSecondaryGoalPickerClick,
                        onTargetClick = contract::onTargetDatePickerClick
                    )
                }

                PersonalizationKeyEnumState.Category.entries.forEach { category ->
                    val items = groupedPersonalization[category] ?: return@forEach
                    if (items.isEmpty()) return@forEach

                    item(key = "personalization_${category.name}") {
                        PersonalizationGroup(
                            modifier = Modifier.fillMaxWidth(),
                            category = category,
                            items = items,
                            selected = state.selectedPersonalization,
                            onSelect = contract::onPersonalizationClick,
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders, state.selectedPrimary) {
                when {
                    loaders.contains(ProfileGoalLoader.SaveButton) -> ButtonState.Loading
                    state.selectedPrimary == null -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.goal_save_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onSave,
            )

            Spacer(Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(Modifier.navigationBarsPadding())
        },
    )
}

@Composable
private fun MainGoalSection(
    modifier: Modifier = Modifier,
    primary: GoalPrimaryGoalEnumState?,
    secondary: GoalSecondaryGoalEnumState?,
    target: DateTimeFormatState,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onTargetClick: () -> Unit,
) {
    Column(
        modifier = modifier.animateContentSize()
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_main_section),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        InputPrimaryGoal(
            modifier = Modifier.fillMaxWidth(),
            value = primary,
            onClick = onPrimaryClick,
        )

        AnimatedVisibility(visible = primary != null) {
            primary?.let { value ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                    Text(
                        modifier = modifier.padding(horizontal = AppTokens.dp.contentPadding.subContent),
                        text = value.description(),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.tertiary,
                    )
                }
            }
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        InputSecondaryGoal(
            modifier = Modifier.fillMaxWidth(),
            value = secondary,
            onClick = onSecondaryClick,
        )

        AnimatedVisibility(visible = secondary != null) {
            secondary?.let { value ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                    Text(
                        modifier = modifier.padding(horizontal = AppTokens.dp.contentPadding.subContent),
                        text = value.description(),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.tertiary,
                    )
                }
            }
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        InputDate(
            modifier = Modifier.fillMaxWidth(),
            value = target,
            placeholder = AppTokens.strings.res(Res.string.goal_target_date_placeholder),
            onClick = onTargetClick,
        )
    }
}

@Composable
private fun PersonalizationGroup(
    modifier: Modifier = Modifier,
    category: PersonalizationKeyEnumState.Category,
    items: ImmutableList<PersonalizationKeyEnumState>,
    selected: ImmutableSet<PersonalizationKeyEnumState>,
    onSelect: (PersonalizationKeyEnumState) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {

        ContentSpliter(
            modifier = Modifier.fillMaxWidth(),
            text = category.label(),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            items.forEach { item ->
                key(item) {
                    val isSelected = remember(selected, item) { item in selected }
                    val provider = remember(item) { { onSelect(item) } }

                    CheckSelectableCard(
                        style = CheckSelectableCardStyle.Small(title = item.label()),
                        isSelected = isSelected,
                        onSelect = provider,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = ProfileGoalState(
                selectedPrimary = GoalPrimaryGoalEnumState.entries.random(),
                selectedSecondary = GoalSecondaryGoalEnumState.entries.random(),
                selectedPersonalization = PersonalizationKeyEnumState.entries
                    .shuffled()
                    .take(6)
                    .toPersistentSet(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenEmptyPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = ProfileGoalState(),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}
