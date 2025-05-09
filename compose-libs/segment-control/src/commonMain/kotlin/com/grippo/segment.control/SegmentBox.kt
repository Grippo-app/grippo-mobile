package com.grippo.segment.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import com.grippo.segment.control.internal.SelectedButtonId

/**
 * A button used as a child of [SegmentedFrame].
 */

@Composable
public fun SegmentBox(
    modifier: Modifier = Modifier,
    selected: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.then(if (selected) Modifier.layoutId(SelectedButtonId) else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
