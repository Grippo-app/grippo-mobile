package com.grippo.training.recording.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.components.training.TrainingStat
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.tonnage

@Composable
internal fun Header(
    modifier: Modifier = Modifier,
    duration: String,
    volume: VolumeFormatState,
    repetitions: RepetitionsFormatState,
) {
    val largeFont = LocalDensity.current.fontScale > AppTokens.dp.screen.largeFontScaleThreshold

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = if (largeFont) 2 else 3,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        TrainingStat(
            modifier = if (largeFont) Modifier.fillMaxWidth() else Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.duration),
            value = AnnotatedString(duration),
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.tonnage),
            value = when (volume) {
                is VolumeFormatState.Empty -> VolumeFormatState.Valid("0", 0f).shortAnnotated()
                else -> volume.shortAnnotated()
            },
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.reps),
            value = when (repetitions) {
                is RepetitionsFormatState.Empty -> RepetitionsFormatState.Valid("0", 0).shortAnnotated()
                else -> repetitions.shortAnnotated()
            },
            contentColor = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun HeaderActivePreview() {
    PreviewContainer {
        Header(
            duration = "00:45:12",
            volume = VolumeFormatState.of(5628f),
            repetitions = RepetitionsFormatState.of(73),
        )
    }
}

@AppPreview
@Composable
private fun HeaderJustStartedPreview() {
    PreviewContainer {
        Header(
            duration = "00:02:08",
            volume = VolumeFormatState.of(150f),
            repetitions = RepetitionsFormatState.of(8),
        )
    }
}

@AppPreview
@Composable
private fun HeaderHeavyPreview() {
    PreviewContainer {
        Header(
            duration = "01:35:42",
            volume = VolumeFormatState.of(12480f),
            repetitions = RepetitionsFormatState.of(96),
        )
    }
}

@AppPreview
@Composable
private fun HeaderEmptyPreview() {
    PreviewContainer {
        Header(
            duration = "00:00:00",
            volume = VolumeFormatState.of(0f),
            repetitions = RepetitionsFormatState.of(0),
        )
    }
}
