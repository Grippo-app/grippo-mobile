package com.grippo.profile.weight.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.weight_history
import com.grippo.presentation.api.profile.models.WeightFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun WeightHistoryScreen(
    state: WeightHistoryState,
    loaders: ImmutableSet<WeightHistoryLoader>,
    contract: WeightHistoryContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.weight_history),
        onBack = contract::back
    )

    InputWeight(
        value = state.weight.value,
        onClick = contract::openWeightPicker
    )
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