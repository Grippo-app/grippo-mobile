package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.muscles.stubMuscles
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayLazyColumn
import com.grippo.design.components.muscle.MusclesColumn
import com.grippo.design.components.muscle.MusclesImage
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.registration_muscles_description
import com.grippo.design.resources.provider.registration_muscles_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExcludedMusclesScreen(
    state: ExcludedMusclesState,
    loaders: ImmutableSet<ExcludedMusclesLoader>,
    contract: ExcludedMusclesContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
        ambient = ScreenBackground.Ambient(
            color = AppTokens.colors.brand.color3,
        )
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        BottomOverlayLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
            overlay = AppTokens.colors.background.screen,
            content = {
                itemsIndexed(
                    state.suggestions,
                    key = { _, item -> item.id },
                ) { index, group ->
                    val isEven = index % 2 == 0
                    val preset = state.musclePresets[group.id] ?: return@itemsIndexed

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEven) {
                            MusclesColumn(
                                modifier = Modifier.weight(1f),
                                item = group,
                                selectedIds = state.selectedMuscleIds,
                                onSelect = contract::onSelect
                            )
                            MusclesImage(
                                modifier = Modifier.weight(1f),
                                item = group,
                                preset = preset
                            )
                        } else {
                            MusclesImage(
                                modifier = Modifier.weight(1f),
                                item = group,
                                preset = preset
                            )
                            MusclesColumn(
                                modifier = Modifier.weight(1f),
                                item = group,
                                selectedIds = state.selectedMuscleIds,
                                onSelect = contract::onSelect
                            )
                        }
                    }
                }
            },
            bottom = {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                Button(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                        .fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.continue_btn),
                    ),
                    style = ButtonStyle.Primary,
                    onClick = contract::onNextClick
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
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
