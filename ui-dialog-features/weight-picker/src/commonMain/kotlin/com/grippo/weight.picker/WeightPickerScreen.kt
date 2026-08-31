package com.grippo.weight.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun WeightPickerScreen(
    state: WeightPickerState,
    loaders: ImmutableSet<WeightPickerLoader>,
    contract: WeightPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        WeightPickerScreen(
            state = WeightPickerState(
                value = WeightFormatState.of(55.4F)
            ),
            contract = WeightPickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
