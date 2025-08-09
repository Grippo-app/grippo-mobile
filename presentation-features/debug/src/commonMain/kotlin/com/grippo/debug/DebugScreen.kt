package com.grippo.debug

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipments
import com.grippo.state.formatters.UiText
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun DebugScreen(
    state: DebugState,
    loaders: ImmutableSet<DebugLoader>,
    contract: DebugContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    val segmentItems = remember {
        DebugMenu.entries.map { it to UiText.Str(it.name) }.toPersistentList()
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.equipments),
        onBack = contract::back,
        content = {
            Segment(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = segmentItems,
                selected = state.selected,
                onSelect = contract::select,
                segmentWidth = SegmentWidth.Unspecified,
            )
        }
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DebugScreen(
            state = DebugState(),
            loaders = persistentSetOf(),
            contract = DebugContract.Empty
        )
    }
}