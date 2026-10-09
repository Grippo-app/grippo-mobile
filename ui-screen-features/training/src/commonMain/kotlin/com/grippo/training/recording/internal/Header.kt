package com.grippo.training.recording.internal

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.duration),
            value = AnnotatedString(duration),
            contentColor = AppTokens.colors.context.duration,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.tonnage),
            value = volume.shortAnnotated(),
            contentColor = AppTokens.colors.context.volume,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.reps),
            value = repetitions.shortAnnotated(),
            contentColor = AppTokens.colors.context.repetitions,
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
