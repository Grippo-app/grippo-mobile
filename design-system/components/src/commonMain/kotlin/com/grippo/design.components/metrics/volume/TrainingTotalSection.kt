package com.grippo.design.components.metrics.volume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingTotalSection(
    modifier: Modifier = Modifier,
    value: TrainingTotalState,
    size: ChipSize = ChipSize.Medium,
) {
    val volume = value.volume
        .takeIf { it.value != null }
    val repetitions = value.repetitions
        .takeIf { it.value != null }

    if (volume == null && repetitions == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        volume?.let { value ->
            VolumeChip(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                style = VolumeChipStyle.LONG,
                size = size,
            )
        }

        repetitions?.let { value ->
            RepetitionsChip(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                style = RepetitionsChipStyle.LONG,
                size = size,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingTotalSectionPreview() {
    val total = trainingTotalPreviewValue()
    PreviewContainer {
        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total,
        )

        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total,
            size = ChipSize.Small,
        )
    }
}

@AppPreview
@Composable
private fun TrainingTotalSectionLargeValuesPreview() {
    val total = trainingTotalPreviewValue()
    PreviewContainer {
        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total.copy(
                volume = VolumeFormatState.of(97_267f),
                repetitions = RepetitionsFormatState.of(1_096),
            ),
        )

        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total.copy(
                volume = VolumeFormatState.of(1_234_567f),
                repetitions = RepetitionsFormatState.of(1_234_567),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingTotalSectionPartialPreview() {
    val total = trainingTotalPreviewValue()
    PreviewContainer {
        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total.copy(repetitions = RepetitionsFormatState.Empty()),
        )

        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = total.copy(volume = VolumeFormatState.Empty()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingTotalSectionEmptyPreview() {
    PreviewContainer {
        TrainingTotalSection(
            modifier = Modifier.fillMaxWidth(),
            value = trainingTotalPreviewValue().copy(
                volume = VolumeFormatState.Empty(),
                repetitions = RepetitionsFormatState.Empty(),
            ),
        )
    }
}

private fun trainingTotalPreviewValue(): TrainingTotalState {
    return stubTotal().copy(
        volume = VolumeFormatState.of(400f),
        repetitions = RepetitionsFormatState.of(10),
        intensity = IntensityFormatState.of(40f),
    )
}
