package com.grippo.language.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.settings.LanguageEnumState
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun LanguagePickerScreen(
    state: LanguagePickerState,
    loaders: ImmutableSet<LanguagePickerLoader>,
    contract: LanguagePickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight = 1f, fill = false),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        overlay = AppTokens.colors.background.dialog,
        content = { resolvedPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = resolvedPadding,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                items(state.languages, key = { it.name }) { language ->
                    CheckSelectableCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = CheckSelectableCardStyle.Medium(
                            title = language.title(),
                            description = null,
                        ),
                        isSelected = state.value == language,
                        onSelect = { contract.onSelectLanguage(language) },
                    )
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun LanguagePickerPreview() {
    PreviewContainer {
        LanguagePickerScreen(
            state = LanguagePickerState(
                value = LanguageEnumState.UKRAINIAN,
            ),
            loaders = persistentSetOf(),
            contract = LanguagePickerContract.Empty,
        )
    }
}
