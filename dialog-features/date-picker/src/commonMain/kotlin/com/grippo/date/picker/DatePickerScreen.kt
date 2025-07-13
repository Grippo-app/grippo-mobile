package com.grippo.date.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.date_picker_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract
) = BaseComposeDialog(ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    Toolbar(
        modifier = Modifier,
        title = AppTokens.strings.res(Res.string.date_picker_title),
        style = ToolbarStyle.Transparent,
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DatePickerScreen(
            state = DatePickerState(
                initial = LocalDateTime(2025, 7, 9, 14, 30),
            ),
            loaders = persistentSetOf(),
            contract = DatePickerContract.Empty
        )
    }
}