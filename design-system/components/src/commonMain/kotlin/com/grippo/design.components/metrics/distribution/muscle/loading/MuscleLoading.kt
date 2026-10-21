package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.MuscleLoadingPerGroup
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.MuscleLoadingPerMuscle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_no_data
import kotlinx.collections.immutable.persistentListOf

@Immutable
public enum class MuscleLoadingMode {
    PerGroup,
    PerMuscle,
}

@Immutable
public enum class MuscleLoadingStyle {
    Annotated,
    Breakdown,
}

@Composable
public fun MuscleLoading(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    mode: MuscleLoadingMode,
    style: MuscleLoadingStyle = MuscleLoadingStyle.Annotated,
) {
    val entries = when (mode) {
        MuscleLoadingMode.PerGroup -> summary.perGroup.entries
        MuscleLoadingMode.PerMuscle -> summary.perMuscle.entries
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        if (entries.isEmpty()) {
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_no_data),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        when (mode) {
            MuscleLoadingMode.PerGroup -> MuscleLoadingPerGroup(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
            )

            MuscleLoadingMode.PerMuscle -> MuscleLoadingPerMuscle(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
            )
        }
        if (style == MuscleLoadingStyle.Annotated) {
            MuscleLoadingDataNotice(summary = summary)
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingBreakdownPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerGroup,
            style = MuscleLoadingStyle.Breakdown,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingIncompletePreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoading(
            summary = summary.copy(
                meta = summary.meta.copy(
                    excludedSets = 2,
                    unassignedSets = 3,
                    unmeasuredVolumeSets = 4,
                ),
            ),
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingEmptyPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoading(
            summary = summary.copy(
                meta = summary.meta.copy(
                    totalExercises = 0,
                    totalSets = 0,
                    totalRepetitions = 0,
                    totalVolume = 0f,
                    dominantGroup = null,
                ),
                perGroup = summary.perGroup.copy(entries = persistentListOf()),
                perMuscle = summary.perMuscle.copy(entries = persistentListOf()),
            ),
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCollapsedPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingExpandedPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerMuscle,
        )
    }
}
