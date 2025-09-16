package com.grippo.state.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.english
import com.grippo.design.resources.provider.icons.FlagEn
import com.grippo.design.resources.provider.icons.FlagUa
import com.grippo.design.resources.provider.ukrainian
import com.grippo.state.formatters.UiText

@Immutable
public enum class LocaleState(public val tag: String) {
    EN("en"),
    UA("ua");

    public fun title(): UiText {
        val r = when (this) {
            EN -> Res.string.english
            UA -> Res.string.ukrainian
        }
        return UiText.Res(r)
    }

    @Composable
    public fun icon(): ImageVector {
        val r = when (this) {
            EN -> AppTokens.icons.FlagEn
            UA -> AppTokens.icons.FlagUa
        }
        return r
    }
}