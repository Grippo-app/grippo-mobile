package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.activity
import com.grippo.design.resources.provider.icons.Edit
import com.grippo.design.resources.provider.profile_menu_excluded_muscles
import com.grippo.design.resources.provider.profile_menu_missing_equipment
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class ProfileMenu {
    Muscles,
    Equipment;

    public companion object Companion {
        @Composable
        public fun title(): String {
            return AppTokens.strings.res(Res.string.activity)
        }
    }

    @Composable
    public fun text(): String {
        return when (this) {
            Muscles -> AppTokens.strings.res(Res.string.profile_menu_excluded_muscles)
            Equipment -> AppTokens.strings.res(Res.string.profile_menu_missing_equipment)
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Muscles -> AppTokens.icons.Edit
            Equipment -> AppTokens.icons.Edit
        }
    }
}
