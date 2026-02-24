package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.debug
import com.grippo.design.resources.provider.follow_us
import com.grippo.design.resources.provider.icons.Bug
import com.grippo.design.resources.provider.icons.ChatAlt
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.more
import com.grippo.design.resources.provider.settings
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class SettingsMenu {
    Settings,
    Social,
    Debug;

    public companion object Companion {
        @Composable
        public fun title(): String {
            return AppTokens.strings.res(Res.string.more)
        }
    }

    @Composable
    public fun text(): String {
        return when (this) {
            Debug -> AppTokens.strings.res(Res.string.debug)
            Settings -> AppTokens.strings.res(Res.string.settings)
            Social -> AppTokens.strings.res(Res.string.follow_us)
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Debug -> AppTokens.icons.Bug
            Settings -> AppTokens.icons.Filter
            Social -> AppTokens.icons.ChatAlt
        }
    }
}
