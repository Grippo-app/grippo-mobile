package com.grippo.profile.weight.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.weight_history
import com.grippo.state.formatters.WeightFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun WeightHistoryScreen(
    state: WeightHistoryState,
    loaders: ImmutableSet<WeightHistoryLoader>,
    contract: WeightHistoryContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.weight_history),
        onBack = contract::back
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .weight(1f)
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
    ) {

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        InputWeight(
            value = state.weight.value,
            onClick = contract::openWeightPicker
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = AppTokens.dp.contentPadding.content
            ),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {

        }

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        WeightHistoryScreen(
            state = WeightHistoryState(
                weight = WeightFormatState.of(33f)
            ),
            loaders = persistentSetOf(),
            contract = WeightHistoryContract.Empty
        )
    }
}