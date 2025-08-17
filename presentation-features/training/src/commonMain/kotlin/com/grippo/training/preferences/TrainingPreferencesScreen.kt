package com.grippo.training.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.MenuCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.muscles.stubMuscles
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingPreferencesScreen(
    state: TrainingPreferencesState,
    loaders: ImmutableSet<TrainingPreferencesLoader>,
    contract: TrainingPreferencesContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = "Training preferences",
    ) {}

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f)
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
    ) {

    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.screen.horizontalPadding)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            style = ButtonStyle.Primary,
            onClick = contract::onContinueClick
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.content))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = "Skip",
            style = ButtonStyle.Transparent,
            onClick = contract::onContinueClick
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingPreferencesScreen(
            state = TrainingPreferencesState(
                muscles = stubMuscles()
            ),
            loaders = persistentSetOf(),
            contract = TrainingPreferencesContract.Empty
        )
    }
}
