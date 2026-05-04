package com.grippo.training.recording.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
public fun Header(
    modifier: Modifier = Modifier,
    duration: String,
    volume: VolumeFormatState,
    repetitions: RepetitionsFormatState,
) {
    val intensityColors = AppTokens.colors.training.intensity
    val volumeColors = AppTokens.colors.training.volume
    val repetitionsColors = AppTokens.colors.training.repetitions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.duration),
            value = duration,
            accentColor = intensityColors.startColor,
            contentColor = intensityColors.contentColor,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.tonnage),
            value = volume.short(),
            accentColor = volumeColors.startColor,
            contentColor = volumeColors.contentColor,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.reps),
            value = repetitions.short(),
            accentColor = repetitionsColors.startColor,
            contentColor = repetitionsColors.contentColor,
        )
    }
}

@AppPreview
@Composable
private fun HeaderActivePreview() {
    PreviewContainer {
        Header(
            duration = "00:45",
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
            duration = "00:02",
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
            duration = "01:35",
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
            duration = "00:00",
            volume = VolumeFormatState.of(0f),
            repetitions = RepetitionsFormatState.of(0),
        )
    }
}
