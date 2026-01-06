package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.icons.Pro
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.icons.Volume
import com.grippo.design.resources.provider.profile
import com.grippo.design.resources.provider.profile_menu_excluded_muscles
import com.grippo.design.resources.provider.profile_menu_experience
import com.grippo.design.resources.provider.profile_menu_missing_equipment
import com.grippo.design.resources.provider.settings
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class ProfileMenu {
    Experience,
    Muscles,
    Equipment,
    Settings;

    public companion object Companion {
        @Composable
        public fun title(): String {
            return AppTokens.strings.res(Res.string.profile)
        }
    }

    @Composable
    public fun text(): String {
        return when (this) {
            Muscles -> AppTokens.strings.res(Res.string.profile_menu_excluded_muscles)
            Equipment -> AppTokens.strings.res(Res.string.profile_menu_missing_equipment)
            Experience -> AppTokens.strings.res(Res.string.profile_menu_experience)
            Settings -> AppTokens.strings.res(Res.string.settings)
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Muscles -> AppTokens.icons.User
            Equipment -> AppTokens.icons.Volume
            Experience -> AppTokens.icons.Pro
            Settings -> AppTokens.icons.Filter
        }
    }
}
