package com.grippo.debug.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun LoggerPage(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val logs = remember {
            AppLogger.logFileContentsByCategory()
        }

        val selectedLog: MutableState<String?> = remember(logs.keys) {
            mutableStateOf(logs.keys.firstOrNull())
        }

        val logItems = remember {
            logs.map { it.key to UiText.Str(it.key) }.toPersistentList()
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        Segment(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            items = logItems,
            selected = selectedLog.value,
            onSelect = { selectedLog.value = it },
            segmentWidth = SegmentWidth.Unspecified,
            style = SegmentStyle.Fill
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        val log = remember(logItems, selectedLog.value) {
            logs[selectedLog.value].orEmpty()
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(log) {
                Text(
                    text = it,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.primary
                )
            }
        }
    }
}