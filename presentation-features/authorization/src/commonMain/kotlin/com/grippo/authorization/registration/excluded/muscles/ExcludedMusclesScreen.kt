package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.muscle.MusclesColumn
import com.grippo.design.components.muscle.MusclesImage
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_muscles_description
import com.grippo.design.resources.registration_muscles_title
import com.grippo.presentation.api.muscles.models.stubMuscles
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExcludedMusclesScreen(
    state: ExcludedMusclesState,
    loaders: ImmutableSet<ExcludedMusclesLoader>,
    contract: ExcludedMusclesContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        onBack = contract::back,
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .weight(1f)
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            itemsIndexed(
                state.suggestions,
                key = { _, item -> item.id }) { index, group ->
                val isEven = index % 2 == 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEven) {
                        MusclesColumn(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds,
                            onSelect = contract::select
                        )
                        MusclesImage(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds
                        )
                    } else {
                        MusclesImage(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds
                        )
                        MusclesColumn(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds,
                            onSelect = contract::select
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            style = ButtonStyle.Primary,
            onClick = contract::next
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewSelected() {
    PreviewContainer {
        ExcludedMusclesScreen(
            state = ExcludedMusclesState(
                suggestions = stubMuscles(),
                selectedMuscleIds = stubMuscles()
                    .map { it.muscles.map { it.value.id } }
                    .flatten()
                    .take(3).toPersistentList()
            ),
            loaders = persistentSetOf(),
            contract = ExcludedMusclesContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewUnselected() {
    PreviewContainer {
        ExcludedMusclesScreen(
            state = ExcludedMusclesState(
                suggestions = stubMuscles(),
                selectedMuscleIds = persistentListOf()
            ),
            loaders = persistentSetOf(),
            contract = ExcludedMusclesContract.Empty
        )
    }
}