package com.grippo.menu.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.menu.PickerMenuItem
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun MenuPickerScreen(
    state: MenuPickerState,
    loaders: ImmutableSet<MenuPickerLoader>,
    contract: MenuPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        MenuPickerScreen(
            state = MenuPickerState(
                items = persistentListOf<PickerMenuItem>(
                    TrainingMenu.Details,
                    TrainingMenu.Edit,
                    TrainingMenu.Delete,
                ),
            ),
            loaders = persistentSetOf(),
            contract = MenuPickerContract.Empty
        )
    }
}
