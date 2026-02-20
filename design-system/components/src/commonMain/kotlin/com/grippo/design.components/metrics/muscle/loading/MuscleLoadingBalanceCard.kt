package com.grippo.design.components.metrics.muscle.loading

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
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_balance
import com.grippo.design.resources.provider.muscle_load_balance_description_1
import com.grippo.design.resources.provider.muscle_load_balance_description_2
import com.grippo.design.resources.provider.muscle_load_balance_score
import com.grippo.design.resources.provider.muscle_load_balance_score_label
import com.grippo.design.resources.provider.muscle_load_balance_status_balanced
import com.grippo.design.resources.provider.muscle_load_balance_status_moderate
import com.grippo.design.resources.provider.muscle_load_balance_status_strong
import com.grippo.design.resources.provider.muscle_load_bias
import com.grippo.design.resources.provider.muscle_load_gap
import com.grippo.design.resources.provider.muscle_load_no_data
import com.grippo.design.resources.provider.muscle_load_trainings_count
import com.grippo.design.resources.provider.percent
import kotlin.math.roundToInt

@Composable
public fun MuscleLoadingBalanceCard(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
) {
    val groupEntries = summary.perGroup.entries
    val topEntry = groupEntries.maxByOrNull { it.value }
    val secondEntry = groupEntries.filterNot { it == topEntry }.maxByOrNull { it.value }
    val gapEntry = groupEntries.minByOrNull { it.hitTrainingsCount }

    val topShare = summary.groupDominance.top1SharePercent.coerceIn(0f, 100f)
    val balanceScore = (100 - topShare).roundToInt().coerceIn(0, 100)

    val statusRes = when {
        balanceScore >= 70 -> Res.string.muscle_load_balance_status_balanced
        balanceScore >= 50 -> Res.string.muscle_load_balance_status_moderate
        else -> Res.string.muscle_load_balance_status_strong
    }

    val status = AppTokens.strings.res(statusRes)
    val percentSymbol = AppTokens.strings.res(Res.string.percent)

    val biasLine = if (topEntry != null && secondEntry != null) {
        val diff = (topEntry.value - secondEntry.value).roundToInt().coerceAtLeast(0)
        AppTokens.strings.res(
            Res.string.muscle_load_bias,
            topEntry.group.title().text(),
            diff
        )
    } else {
        null
    }

    val gapLine = if (gapEntry != null) {
        val trainingsCount = AppTokens.strings.res(
            Res.string.muscle_load_trainings_count,
            gapEntry.hitTrainingsCount
        )
        AppTokens.strings.res(
            Res.string.muscle_load_gap,
            gapEntry.group.title().text(),
            trainingsCount
        )
    } else {
        null
    }

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
                        color = AppTokens.colors.semantic.warning
                    )
                } else {
                    Text(
                        text = AppTokens.strings.res(Res.string.muscle_load_no_data),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.tertiary
                    )
                }

                if (gapLine != null) {
                    Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                    Text(
                        text = gapLine,
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.primary
                    )
                }

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
            }

            BalanceRing(
                modifier = Modifier.size(AppTokens.dp.metrics.muscleLoad.balance.chart),
                score = balanceScore,
                caption = AppTokens.strings.res(Res.string.muscle_load_balance_score_label),
                percentSymbol = percentSymbol
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_balance_description_1),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary
            )

            Text(
                text = AppTokens.strings.res(Res.string.muscle_load_balance_description_2),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}

@Composable
private fun BalanceRing(
    score: Int,
    caption: String,
    percentSymbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = when {
        score >= 70 -> AppTokens.colors.lineIndicator.success
        score >= 50 -> AppTokens.colors.lineIndicator.info
        else -> AppTokens.colors.lineIndicator.warning
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score$percentSymbol",
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )
            Text(
                text = caption,
                style = AppTokens.typography.b12Med(),
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
