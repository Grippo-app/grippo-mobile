package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_excluded_sets
import com.grippo.design.resources.provider.muscle_load_unassigned_sets
import com.grippo.design.resources.provider.muscle_load_unmeasured_volume_sets

@Composable
internal fun MuscleLoadingDataNotice(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    if (summary.meta.excludedSets == 0 &&
        summary.meta.unassignedSets == 0 &&
        summary.meta.unmeasuredVolumeSets == 0
    ) {
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        if (summary.meta.excludedSets > 0) {
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_excluded_sets, summary.meta.excludedSets),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (summary.meta.unassignedSets > 0) {
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_unassigned_sets, summary.meta.unassignedSets),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (summary.meta.unmeasuredVolumeSets > 0) {
            Text(
                text = AppTokens.strings.res(
                    Res.string.muscle_load_unmeasured_volume_sets,
                    summary.meta.unmeasuredVolumeSets,
                ),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}
