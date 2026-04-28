package com.grippo.design.components.metrics.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState
import com.grippo.core.state.metrics.profile.TrainingProfileKindState
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfile
import com.grippo.design.components.tip.TipCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_summary_confidence_label
import com.grippo.design.resources.provider.training_profile_summary_dominant_label
import com.grippo.design.resources.provider.training_profile_summary_no_dominant

@Composable
public fun TrainingProfileSummaryCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
) {
    Column(
        modifier = modifier
            .background(
                AppTokens.colors.background.card,
                RoundedCornerShape(AppTokens.dp.metrics.profile.trainingProfile.details.radius)
            ).padding(
                horizontal = AppTokens.dp.metrics.profile.trainingProfile.details.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.trainingProfile.details.verticalPadding
            ),
    ) {
        Text(
            text = value.title(),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = value.subtitle(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        // Pills only render when the profile actually has a meaningful
        // verdict — Easy sessions clear both, so the row collapses cleanly.
        val hasDominant = value.dominant != null
        val hasConfidence = value.confidence > 0 && value.kind != TrainingProfileKindState.Easy
        if (hasDominant || hasConfidence) {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Pill(
                    label = AppTokens.strings.res(Res.string.training_profile_summary_dominant_label),
                    value = value.dominant?.label()
                        ?: AppTokens.strings.res(Res.string.training_profile_summary_no_dominant),
                    color = AppTokens.colors.semantic.info,
                )

                if (hasConfidence) {
                    Pill(
                        label = AppTokens.strings.res(
                            Res.string.training_profile_summary_confidence_label,
                            value.confidence,
                        ),
                        value = null,
                        color = confidenceColor(value.confidence),
                    )
                }
            }
        }

        value.tip()?.let { tip ->
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            TipCard(
                modifier = Modifier.fillMaxWidth(),
                value = tip
            )
        }
    }
}

@Composable
private fun Pill(
    modifier: Modifier = Modifier,
    label: String,
    value: String?,
    color: Color,
) {
    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)
    Text(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.20f), shape = shape)
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding,
            ),
        text = if (value != null) "$label: $value" else label,
        style = AppTokens.typography.b11Semi(),
        color = color,
    )
}

@Composable
private fun confidenceColor(confidence: Int): Color = when {
    confidence >= 70 -> AppTokens.colors.semantic.success
    confidence >= 40 -> AppTokens.colors.semantic.warning
    else -> AppTokens.colors.semantic.error
}

@AppPreview
@Composable
private fun TrainingProfileSummaryCardPreview() {
    PreviewContainer {
        TrainingProfileSummaryCard(
            value = stubTrainingLoadProfile(),
        )
    }
}
