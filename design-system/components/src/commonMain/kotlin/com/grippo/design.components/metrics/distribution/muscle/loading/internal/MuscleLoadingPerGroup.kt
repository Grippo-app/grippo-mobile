package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummarySingleGroup
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MuscleLoadingPerGroup(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val entries = summary.perGroup.entries
    val highestShare = entries.maxOfOrNull { it.value }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        entries.forEach { entry ->
            key(entry.group) {
                MuscleLoadingItem(
                    entry = entry,
                    label = entry.group.title().text(),
                    color = summary.perGroup.color(entry),
                    dominant = entry.value == highestShare,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerGroupPreview() {
    PreviewContainer {
        MuscleLoadingPerGroup(
            summary = stubMuscleLoadSummary(),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerGroupReorderedPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingPerGroup(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = summary.perGroup.entries.reversed().toPersistentList()),
            ),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerGroupTiedPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingPerGroup(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(
                    entries = summary.perGroup.entries.take(2).map { it.copy(value = 50f) }.toPersistentList(),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPerGroupSinglePreview() {
    PreviewContainer {
        MuscleLoadingPerGroup(summary = stubMuscleLoadSummarySingleGroup())
    }
}
