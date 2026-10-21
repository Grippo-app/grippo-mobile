package com.grippo.secondary.goal.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SecondaryGoalPickerScreen(
    state: SecondaryGoalPickerState,
    loaders: ImmutableSet<SecondaryGoalPickerLoader>,
    contract: SecondaryGoalPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight = 1f, fill = false),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        overlay = AppTokens.colors.background.dialog,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxWidth(),
                contentPadding = resolvedPadding,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                items(state.goals, key = { it.ordinal }) { goal ->
                    CheckSelectableCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = CheckSelectableCardStyle.Large(
                            title = goal.label(),
                            description = goal.description(),
                            icon = goal.icon(),
                        ),
                        isSelected = state.value == goal,
                        onSelect = { contract.onSelectGoal(goal) },
                    )
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        SecondaryGoalPickerScreen(
            state = SecondaryGoalPickerState(
                value = GoalSecondaryGoalEnumState.CONSISTENCY,
            ),
            loaders = persistentSetOf(),
            contract = SecondaryGoalPickerContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun UnselectedGoalsPreview() {
    PreviewContainer {
        SecondaryGoalPickerScreen(
            state = SecondaryGoalPickerState(value = null),
            loaders = persistentSetOf(),
            contract = SecondaryGoalPickerContract.Empty,
        )
    }
}
