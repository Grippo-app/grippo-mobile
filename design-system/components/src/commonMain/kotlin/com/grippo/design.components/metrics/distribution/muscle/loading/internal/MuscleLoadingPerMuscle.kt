package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MuscleLoadingPerMuscle(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val entries = summary.perMuscle.entries
    val highestShare = entries.maxOfOrNull { it.value }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        entries.forEach { entry ->
            key(entry.group, entry.muscles) {
                val muscle = entry.muscles.firstOrNull()
                MuscleLoadingItem(
                    entry = entry,
                    label = muscle?.title()?.text() ?: "-",
                    color = summary.perMuscle.color(entry),
                    muscle = muscle,
                    dominant = entry.value == highestShare,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerMusclePreview() {
    PreviewContainer {
        MuscleLoadingPerMuscle(
            summary = stubMuscleLoadSummary(),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerMuscleEqualPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingPerMuscle(
            summary = summary.copy(
                perMuscle = summary.perMuscle.copy(
                    entries = summary.perMuscle.entries.take(2).map { it.copy(value = 50f) }.toPersistentList(),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerMuscleReorderedPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingPerMuscle(
            summary = summary.copy(
                perMuscle = summary.perMuscle.copy(entries = summary.perMuscle.entries.reversed().toPersistentList()),
            ),
        )
    }
}
