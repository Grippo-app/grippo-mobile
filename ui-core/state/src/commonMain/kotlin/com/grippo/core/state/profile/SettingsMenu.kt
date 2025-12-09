package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.debug
import com.grippo.design.resources.provider.icons.Bug
import com.grippo.design.resources.provider.settings
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class SettingsMenu {
    Debug;

    public companion object Companion {
        @Composable
        public fun title(): String {
            return AppTokens.strings.res(Res.string.settings)
        }
    }

    @Composable
    public fun text(): String {
        return when (this) {
            Debug -> AppTokens.strings.res(Res.string.debug)
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Debug -> AppTokens.icons.Bug
        }
    }
}
