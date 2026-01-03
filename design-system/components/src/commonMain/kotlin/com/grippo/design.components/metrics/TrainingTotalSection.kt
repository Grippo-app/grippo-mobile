package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.metrics.stubTotal
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
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
    state: TrainingTotalState,
    chipSize: ChipSize = ChipSize.Medium,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.summary.spacing)
    ) {
        state.volume
            .takeIf { it.value != null }
            ?.let { value ->
                VolumeChip(
                    modifier = Modifier.weight(1f),
                    value = value,
                    style = VolumeChipStyle.SHORT,
                    size = chipSize,
                )
            }

        state.repetitions
            .takeIf { it.value != null }
            ?.let { value ->
                RepetitionsChip(
                    modifier = Modifier.weight(1f),
                    value = value,
                    style = RepetitionsChipStyle.SHORT,
                    size = chipSize,
                )
            }

        state.intensity
            .takeIf { it.value != null }
            ?.let { value ->
                IntensityChip(
                    modifier = Modifier.weight(1f),
                    value = value,
                    style = IntensityChipStyle.SHORT,
                    size = chipSize,
                )
            }
    }
}

@AppPreview
@Composable
private fun TrainingTotalSectionPreview() {
    PreviewContainer {
        TrainingTotalSection(state = stubTotal())
    }
}
