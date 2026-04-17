package com.grippo.profile.goal

import androidx.compose.foundation.layout.Arrangement
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
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
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
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_picker_primary_title
import com.grippo.design.resources.provider.goal_save_btn
import com.grippo.design.resources.provider.goal_secondary_section
import com.grippo.design.resources.provider.goal_target_date_section
import com.grippo.design.resources.provider.goal_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
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

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
    )

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
                item(key = "primary_goal") {
                    Text(
                        text = AppTokens.strings.res(Res.string.goal_picker_primary_title),
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

                    InputPrimaryGoal(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.selectedPrimary,
                        onClick = contract::onPrimaryGoalPickerClick,
                    )
                }

                item(key = "secondary_goal") {
                    Text(
                        text = AppTokens.strings.res(Res.string.goal_secondary_section),
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

                    InputSecondaryGoal(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.selectedSecondary,
                        onClick = contract::onSecondaryGoalPickerClick,
                    )
                }

                item(key = "target_date") {
                    Text(
                        text = AppTokens.strings.res(Res.string.goal_target_date_section),
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

                    InputDate(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.selectedTarget,
                        onClick = contract::onTargetDatePickerClick,
                    )
                }

                item(key = "personalization") {
                    Text(
                        text = "Personalization",
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {
                        state.personalization.onEach {
                            key(it) {
                                val isSelected = remember(state.selectedPersonalization) {
                                    state.selectedPersonalization.contains(it)
                                }

                                val onSelectProvider = remember(it.ordinal) {
                                    { contract.onPersonalizationClick(it) }
                                }

                                CheckSelectableCard(
                                    style = CheckSelectableCardStyle.Small(
                                        title = it.label(),
                                    ),
                                    isSelected = isSelected,
                                    onSelect = onSelectProvider
                                )
                            }
                        }
                    }
                }
            }
        },
        bottom = {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders) {
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
                    .take(3)
                    .toPersistentSet()
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}
