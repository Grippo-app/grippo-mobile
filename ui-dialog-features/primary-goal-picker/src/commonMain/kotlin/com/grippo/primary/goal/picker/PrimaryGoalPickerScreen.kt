package com.grippo.primary.goal.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PrimaryGoalPickerScreen(
    state: PrimaryGoalPickerState,
    loaders: ImmutableSet<PrimaryGoalPickerLoader>,
    contract: PrimaryGoalPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = state.title,
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
    ) {
        items(state.goals, key = { it.ordinal }) { goal ->
            val onClickProvider = remember(goal) {
                { contract.onSelectGoal(goal) }
            }
            CheckSelectableCard(
                modifier = Modifier.fillMaxWidth(),
                style = CheckSelectableCardStyle.Medium(
                    title = goal.label(),
                    description = goal.description()
                ),
                isSelected = state.value == goal,
                onSelect = onClickProvider,
            )
        }

        item(key = "bottom_spacer") {

            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PrimaryGoalPickerScreen(
            state = PrimaryGoalPickerState(
                title = "What's your main goal?",
                value = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
            ),
            loaders = persistentSetOf(),
            contract = PrimaryGoalPickerContract.Empty,
        )
    }
}
