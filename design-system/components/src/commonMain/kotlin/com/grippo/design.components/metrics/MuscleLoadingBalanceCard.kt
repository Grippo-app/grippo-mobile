package com.grippo.design.components.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_balance
import com.grippo.design.resources.provider.muscle_load_balance_description
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
    val shape = RoundedCornerShape(AppTokens.dp.metrics.panel.large.radius)
    val panelPadding = AppTokens.dp.metrics.panel.large

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

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = panelPadding.horizontalPadding,
                vertical = panelPadding.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = AppTokens.strings.res(Res.string.muscle_load_balance),
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary
                    )
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.muscle_load_balance_score,
                            balanceScore
                        ),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary
                    )
                }

                Text(
                    text = status,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.tertiary
                )

                if (biasLine != null) {
                    Text(
                        text = biasLine,
                        style = AppTokens.typography.b13Med(),
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
                    Text(
                        text = gapLine,
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.semantic.info
                    )
                }
            }

            BalanceRing(
                modifier = Modifier.size(100.dp),
                score = balanceScore,
                caption = AppTokens.strings.res(Res.string.muscle_load_balance_score_label),
                percentSymbol = percentSymbol
            )
        }

        Text(
            text = AppTokens.strings.res(Res.string.muscle_load_balance_description),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary
        )
    }
}

@Composable
private fun BalanceRing(
    score: Int,
    caption: String,
    percentSymbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = (score / 100f).coerceIn(0f, 1f)
    val colors = when {
        score >= 70 -> AppTokens.colors.lineIndicator.success
        score >= 50 -> AppTokens.colors.lineIndicator.info
        else -> AppTokens.colors.lineIndicator.warning
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.12f
            val arcSize = size.minDimension - stroke
            val offset = (size.minDimension - arcSize) / 2f
            val topLeft = androidx.compose.ui.geometry.Offset(offset, offset)
            val arcSizePx = androidx.compose.ui.geometry.Size(arcSize, arcSize)

            drawArc(
                color = colors.track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSizePx,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = colors.indicator,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSizePx,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

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
                color = AppTokens.colors.text.tertiary
            )
        }
    }
}