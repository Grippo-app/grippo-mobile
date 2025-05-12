package com.grippo.presentation.api.user.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.experience_advanced_description
import com.grippo.design.resources.experience_advanced_title
import com.grippo.design.resources.experience_beginner_description
import com.grippo.design.resources.experience_beginner_title
import com.grippo.design.resources.experience_intermediate_description
import com.grippo.design.resources.experience_intermediate_title
import com.grippo.design.resources.experience_pro_description
import com.grippo.design.resources.experience_pro_title
import com.grippo.design.resources.icons.Flag
import com.grippo.design.resources.icons.Settings
import com.grippo.design.resources.icons.Star
import com.grippo.design.resources.icons.User

@Immutable
public enum class ExperienceEnumState {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO;

    @Composable
    public fun title(): String {
        return when (this) {
            BEGINNER -> AppTokens.strings.res(Res.string.experience_beginner_title)
            INTERMEDIATE -> AppTokens.strings.res(Res.string.experience_intermediate_title)
            ADVANCED -> AppTokens.strings.res(Res.string.experience_advanced_title)
            PRO -> AppTokens.strings.res(Res.string.experience_pro_title)
        }
    }

    @Composable
    public fun description(): String {
        return when (this) {
            BEGINNER -> AppTokens.strings.res(Res.string.experience_beginner_description)
            INTERMEDIATE -> AppTokens.strings.res(Res.string.experience_intermediate_description)
            ADVANCED -> AppTokens.strings.res(Res.string.experience_advanced_description)
            PRO -> AppTokens.strings.res(Res.string.experience_pro_description)
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            BEGINNER -> AppTokens.icons.User
            INTERMEDIATE -> AppTokens.icons.Settings
            ADVANCED -> AppTokens.icons.Star
            PRO -> AppTokens.icons.Flag
        }
    }
}