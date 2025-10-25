package com.grippo.debug.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.UiText
import com.grippo.debug.DebugContract
import com.grippo.debug.DebugLoader
import com.grippo.debug.LoggerState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.clear_logs
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun LoggerPage(
    modifier: Modifier = Modifier,
    state: LoggerState,
    contract: DebugContract,
    loaders: ImmutableSet<DebugLoader>
) {
    if (loaders.contains(DebugLoader.Logs)) {
        Loader(modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = modifier) {
        val logItems = remember(state.categories) {
            state.categories.map { it to UiText.Str(it) }.toPersistentList()
        }

        Segment(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            items = logItems,
            selected = state.selectedCategory,
            onSelect = contract::onSelectLogCategory,
            segmentWidth = SegmentWidth.Unspecified,
            style = SegmentStyle.Outline
        )

        val logs = state.logs

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(
                items = logs,
                key = { index, _ -> "${state.selectedCategory.orEmpty()}-$index" }
            ) { _, log ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = log,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.primary
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            content = ButtonContent.Text(AppTokens.strings.res(Res.string.clear_logs)),
            style = ButtonStyle.Error,
            onClick = contract::clearLogs
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
