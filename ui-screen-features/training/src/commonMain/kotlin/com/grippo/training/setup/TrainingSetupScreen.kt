package com.grippo.training.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.muscle.MusclesColumn
import com.grippo.design.components.muscle.MusclesImage
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.prepare_training
import com.grippo.design.resources.provider.skip_btn
import com.grippo.state.muscles.stubMuscles
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingSetupScreen(
    state: TrainingSetupState,
    loaders: ImmutableSet<TrainingSetupLoader>,
    contract: TrainingSetupContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.prepare_training),
        onBack = contract::onBack,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        itemsIndexed(
            state.suggestions,
            key = { _, item -> item.id }) { index, group ->
            val isEven = index % 2 == 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEven) {
                    MusclesColumn(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds,
                        onSelect = contract::onSelect
                    )
                    MusclesImage(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds
                    )
                } else {
                    MusclesImage(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds
                    )
                    MusclesColumn(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds,
                        onSelect = contract::onSelect
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders, state.selectedMuscleIds) {
        when {
            state.selectedMuscleIds.isEmpty() -> ButtonState.Disabled
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.continue_btn),
        style = ButtonStyle.Primary,
        state = buttonState,
        onClick = contract::onContinueClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.skip_btn),
        style = ButtonStyle.Secondary,
        onClick = contract::onContinueClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingSetupScreen(
            state = TrainingSetupState(
                suggestions = stubMuscles()
            ),
            loaders = persistentSetOf(),
            contract = TrainingSetupContract.Empty
        )
    }
}