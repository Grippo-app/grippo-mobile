package com.grippo.training.profile.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.TrainingProfileInsightSeverityState
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfile
import com.grippo.design.components.metrics.profile.ProfileInsightCard
import com.grippo.design.components.metrics.profile.ProfileInsightSeverity
import com.grippo.design.components.metrics.profile.TrainingProfileDimensionsCard
import com.grippo.design.components.metrics.profile.TrainingProfileMovementStyleCard
import com.grippo.design.components.metrics.profile.TrainingProfileMuscleFocusCard
import com.grippo.design.components.metrics.profile.TrainingProfileRadar
import com.grippo.design.components.metrics.profile.TrainingProfileRadarStyle
import com.grippo.design.components.metrics.profile.TrainingProfileSummaryCard
import com.grippo.design.components.metrics.profile.TrainingProfileTopExercisesCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile
import com.grippo.design.resources.provider.training_profile_artifact_movement_style_title
import com.grippo.design.resources.provider.training_profile_artifact_muscle_focus_title
import com.grippo.design.resources.provider.training_profile_artifact_top_exercises_title
import com.grippo.design.resources.provider.training_profile_details_subtitle
import com.grippo.design.resources.provider.training_profile_dimensions_title
import com.grippo.design.resources.provider.training_profile_drifting_title
import com.grippo.design.resources.provider.training_profile_on_track_title
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingProfileDetailsScreen(
    state: TrainingProfileDetailsDialogState,
    loaders: ImmutableSet<TrainingProfileDetailsLoader>,
    contract: TrainingProfileDetailsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    val baseTitle = AppTokens.strings.res(Res.string.training_profile)

    val title = state.range.label()?.text()
        ?.let { "$baseTitle • $it" }
        ?: baseTitle

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        text = AppTokens.strings.res(Res.string.training_profile_details_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = state.range.display,
        style = AppTokens.typography.b13Med(),
        color = AppTokens.colors.text.tertiary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val listState = rememberAnchoredLazyListState(
        behavior = AnchorScrollBehavior.Animated
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        state.profile?.let { profile ->
            item(key = "radar") {
                TrainingProfileRadar(
                    value = profile,
                    style = TrainingProfileRadarStyle.LARGE
                )
            }

            item(key = "summary") {
                TrainingProfileSummaryCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile,
                )
            }

            item(key = "dimensions_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.training_profile_dimensions_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
            item(key = "dimensions") {
                TrainingProfileDimensionsCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile,
                )
            }

            item(key = "movement_style_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.training_profile_artifact_movement_style_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }

            item(key = "movement_style") {
                TrainingProfileMovementStyleCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }

            item(key = "top_exercises_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.training_profile_artifact_top_exercises_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
            item(key = "top_exercises") {
                TrainingProfileTopExercisesCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }

            item(key = "muscle_focus_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.training_profile_artifact_muscle_focus_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
            item(key = "muscle_focus") {
                TrainingProfileMuscleFocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }

            val drifting = profile.insights.filter {
                it.severity == TrainingProfileInsightSeverityState.Warning ||
                        it.severity == TrainingProfileInsightSeverityState.Negative
            }
            val onTrack = profile.insights.filter {
                it.severity == TrainingProfileInsightSeverityState.Positive ||
                        it.severity == TrainingProfileInsightSeverityState.Neutral
            }

            if (drifting.isNotEmpty()) {
                item(key = "drifting_header") {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = AppTokens.strings.res(Res.string.training_profile_drifting_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }
                itemsIndexed(
                    items = drifting,
                    key = { index, item -> "drifting_${item.reason.name}_$index" },
                ) { _, item ->
                    ProfileInsightCard(
                        modifier = Modifier.fillMaxWidth(),
                        severity = item.severity.toInsightSeverity(),
                        headline = item.reason.headline(),
                        detail = item.reason.detail(),
                        action = item.action?.text(),
                    )
                }
            }

            if (onTrack.isNotEmpty()) {
                item(key = "on_track_header") {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = AppTokens.strings.res(Res.string.training_profile_on_track_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }
                itemsIndexed(
                    items = onTrack,
                    key = { index, item -> "on_track_${item.reason.name}_$index" },
                ) { _, item ->
                    ProfileInsightCard(
                        modifier = Modifier.fillMaxWidth(),
                        severity = item.severity.toInsightSeverity(),
                        headline = item.reason.headline(),
                        detail = item.reason.detail(),
                        action = item.action?.text(),
                    )
                }
            }
        }

        item("bottom_space") {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private fun TrainingProfileInsightSeverityState.toInsightSeverity(): ProfileInsightSeverity =
    when (this) {
        TrainingProfileInsightSeverityState.Positive -> ProfileInsightSeverity.Positive
        TrainingProfileInsightSeverityState.Warning -> ProfileInsightSeverity.Warning
        TrainingProfileInsightSeverityState.Negative -> ProfileInsightSeverity.Negative
        TrainingProfileInsightSeverityState.Neutral -> ProfileInsightSeverity.Neutral
    }

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingProfileDetailsScreen(
            state = TrainingProfileDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                profile = stubTrainingLoadProfile()
            ),
            loaders = persistentSetOf(),
            contract = TrainingProfileDetailsContract.Empty
        )
    }
}
