package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_balance
import com.grippo.design.resources.provider.muscle_load_balance_score
import com.grippo.design.resources.provider.muscle_load_balance_score_label
import com.grippo.design.resources.provider.muscle_load_balance_status_balanced
import com.grippo.design.resources.provider.muscle_load_balance_status_moderate
import com.grippo.design.resources.provider.muscle_load_balance_status_strong
import com.grippo.design.resources.provider.muscle_load_bias
import com.grippo.design.resources.provider.muscle_load_gap
import com.grippo.design.resources.provider.muscle_load_no_data
import com.grippo.design.resources.provider.muscle_load_single_group
import com.grippo.design.resources.provider.muscle_load_trainings_count
import com.grippo.design.resources.provider.percent
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
public fun MuscleLoadingBalanceCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val groupEntries = summary.perGroup.entries
    if (groupEntries.isEmpty()) {
        MetricSectionPanel(
            modifier = modifier,
            style = MetricSectionPanelStyle.Small,
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_balance),
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_no_data),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
            )
        }
        return
    }

    val topEntry = groupEntries.maxBy { it.value }
    val secondEntry = groupEntries.filterNot { it == topEntry }.maxByOrNull { it.value }
    val gapEntry = groupEntries.minBy { it.hitTrainingsCount }

    val topShare = summary.groupDominance.top1SharePercent.coerceIn(0f, 100f)
    val balanceScore = (100 - topShare).roundToInt().coerceIn(0, 100)

    val statusRes = when {
        balanceScore >= 70 -> Res.string.muscle_load_balance_status_balanced
        balanceScore >= 50 -> Res.string.muscle_load_balance_status_moderate
        else -> Res.string.muscle_load_balance_status_strong
    }

    val status = AppTokens.strings.res(statusRes)

    val biasLine = if (secondEntry != null) {
        val diff = (topEntry.value - secondEntry.value).roundToInt().coerceAtLeast(0)
        AppTokens.strings.res(
            Res.string.muscle_load_bias,
            topEntry.group.title().text(),
            diff
        )
    } else {
        null
    }

    val trainingsCount = AppTokens.strings.res(
        Res.string.muscle_load_trainings_count,
        gapEntry.hitTrainingsCount,
    )
    val gapLine = AppTokens.strings.res(
        Res.string.muscle_load_gap,
        gapEntry.group.title().text(),
        trainingsCount,
    )

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.muscle_load_balance_score,
                            balanceScore
                        ),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary
                    )

                    Text(
                        text = AppTokens.strings.res(Res.string.muscle_load_balance),
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary
                    )
                }

                Text(
                    text = status,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.tertiary
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

                if (biasLine != null) {
                    Text(
                        text = biasLine,
                        style = AppTokens.typography.b13Semi(),
                        color = AppTokens.colors.text.secondary
                    )
                } else {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.muscle_load_single_group,
                            topEntry.group.title().text(),
                        ),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }

                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    text = gapLine,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.primary
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
            }

            BalanceRing(
                modifier = Modifier.size(AppTokens.dp.metrics.distribution.muscleLoad.balance.chart),
                score = balanceScore,
                caption = AppTokens.strings.res(Res.string.muscle_load_balance_score_label),
            )
        }
    }
}

@Composable
private fun BalanceRing(
    modifier: Modifier = Modifier,
    score: Int,
    caption: String,
) {
    val colors = when {
        score >= 70 -> AppTokens.colors.charts.ring.success
        score >= 50 -> AppTokens.colors.charts.ring.info
        else -> AppTokens.colors.charts.ring.warning
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = score.toFloat(),
            max = 100f,
            colors = colors
        )

        val percent = AppTokens.strings.res(Res.string.percent)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        AppTokens.typography.h3().toSpanStyle()
                            .copy(color = AppTokens.colors.text.primary)
                    ) { append(score.toString()) }
                    withStyle(
                        AppTokens.typography.h6().toSpanStyle()
                            .copy(color = AppTokens.colors.text.secondary)
                    ) { append(percent) }
                },
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                text = caption,
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingBalanceCardPreview() {
    PreviewContainer {
        MuscleLoadingBalanceCard(
            summary = stubMuscleLoadSummary()
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingBalanceCardSingleGroupPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first {
            it.group == MuscleGroupEnumState.ARMS_AND_FOREARMS
        }.copy(value = 100f)
        MuscleLoadingBalanceCard(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = persistentListOf(entry)),
                groupDominance = summary.groupDominance.copy(
                    top1SharePercent = 100f,
                    top2SharePercent = 100f,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingBalanceCardEmptyPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        MuscleLoadingBalanceCard(
            summary = summary.copy(
                perGroup = summary.perGroup.copy(entries = persistentListOf()),
                groupDominance = summary.groupDominance.copy(
                    top1SharePercent = 0f,
                    top2SharePercent = 0f,
                ),
            ),
        )
    }
}
