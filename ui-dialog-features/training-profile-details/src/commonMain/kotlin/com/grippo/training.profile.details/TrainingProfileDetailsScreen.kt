package com.grippo.training.profile.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.components.metrics.training.profile.TrainingLoadProfileDetailsCard
import com.grippo.design.components.metrics.training.profile.TrainingProfileMovementStyleCard
import com.grippo.design.components.metrics.training.profile.TrainingProfileMuscleFocusCard
import com.grippo.design.components.metrics.training.profile.TrainingProfileRadar
import com.grippo.design.components.metrics.training.profile.TrainingProfileRadarStyle
import com.grippo.design.components.metrics.training.profile.TrainingProfileTopExercisesCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile
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
        modifier = Modifier.fillMaxWidth(),
        text = state.range.display,
        style = AppTokens.typography.b14Semi(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    LazyColumn(
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

            item(key = "details") {
                TrainingLoadProfileDetailsCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile,
                )
            }

            item(key = "artifact_top_exercises") {
                TrainingProfileTopExercisesCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }

            item(key = "artifact_muscle_focus") {
                TrainingProfileMuscleFocusCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }

            item(key = "artifact_movement_style") {
                TrainingProfileMovementStyleCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = profile.artifacts,
                )
            }
        }

        item("bottom_space") {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
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
