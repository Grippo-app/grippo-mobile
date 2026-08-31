package com.grippo.duration.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun DurationPickerScreen(
    state: DurationPickerState,
    loaders: ImmutableSet<DurationPickerLoader>,
    contract: DurationPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DurationPickerScreen(
            state = DurationPickerState(
                value = DurationFormatState.of(1.hours + 30.minutes)
            ),
            contract = DurationPickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
