package com.grippo.drart.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.clear_btn
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.draft_training_alert_description_add
import com.grippo.design.resources.provider.draft_training_alert_description_edit
import com.grippo.design.resources.provider.draft_training_alert_title_add
import com.grippo.design.resources.provider.draft_training_alert_title_edit
import com.grippo.design.resources.provider.icons.QuestionMarkCircleOutline
import com.grippo.state.stage.StageState
import com.grippo.state.trainings.stubExercises
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DraftTrainingScreen(
    state: DraftTrainingState,
    loaders: ImmutableSet<DraftTrainingLoader>,
    contract: DraftTrainingContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Icon(
            modifier = Modifier.size(AppTokens.dp.confirmation.icon),
            imageVector = AppTokens.icons.QuestionMarkCircleOutline,
            tint = AppTokens.colors.semantic.info,
            contentDescription = null
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (state.stage) {
                StageState.Add -> AppTokens.strings.res(Res.string.draft_training_alert_title_add)
                StageState.Draft -> AppTokens.strings.res(Res.string.draft_training_alert_title_add)
                is StageState.Edit -> AppTokens.strings.res(Res.string.draft_training_alert_title_edit)
            },
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (state.stage) {
                StageState.Add -> AppTokens.strings.res(Res.string.draft_training_alert_description_add)
                StageState.Draft -> AppTokens.strings.res(Res.string.draft_training_alert_description_add)
                is StageState.Edit -> AppTokens.strings.res(Res.string.draft_training_alert_description_edit)
            },
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        if (state.exercises.isNotEmpty()) LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f, false),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            items(state.exercises, key = { it.id }) { item ->
                ExerciseCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = item,
                    style = ExerciseCardStyle.Small {},
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                AppTokens.dp.contentPadding.content
            )
        ) {
            Button(
                modifier = Modifier.weight(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.clear_btn),
                ),
                style = ButtonStyle.Error,
                onClick = contract::onDelete
            )

            Button(
                modifier = Modifier.weight(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.continue_btn),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onContinue
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreviewAdd() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(
                exercises = stubExercises(),
                stage = StageState.Add
            ),
            contract = DraftTrainingContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEdit() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(
                exercises = stubExercises(),
                stage = StageState.Edit("")
            ),
            contract = DraftTrainingContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
